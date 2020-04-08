module "image_inferrer_queue" {
  source          = "git::github.com/wellcomecollection/terraform-aws-sqs//queue?ref=v1.1.2"
  queue_name      = "${local.namespace_hyphen}_image_inferrer"
  topic_arns      = [module.image_id_minter_topic.arn]
  aws_region      = var.aws_region
  alarm_topic_arn = var.dlq_alarm_arn
}

locals {
  inferrer_host                  = "localhost"
  inferrer_port                  = 80
  inferrer_model_key_config_name = "lsh_model"
  logstash_port                  = 514
}

module "image_inferrer" {
  source = "../modules/inference_service"

  service_name                      = "${local.namespace_hyphen}_image_inferrer"
  inference_manager_container_image = local.inference_manager_image
  inferrer_container_image          = local.feature_inferrer_image
  security_group_ids = [
    aws_security_group.service_egress.id,
    aws_security_group.interservice.id
  ]

  cluster_name = aws_ecs_cluster.cluster.name
  cluster_arn  = aws_ecs_cluster.cluster.arn

  namespace_id = aws_service_discovery_private_dns_namespace.namespace.id

  inference_manager_env_vars = {
    inferrer_host        = local.inferrer_host
    inferrer_port        = local.inferrer_port
    metrics_namespace    = "${local.namespace_hyphen}_image_inferrer"
    topic_arn            = module.image_inferrer_topic.arn
    messages_bucket_name = aws_s3_bucket.messages.id
    queue_url            = module.image_inferrer_queue.url
    logstash_host        = local.logstash_host
  }
  inferrer_env_vars = {
    LOGSTASH_HOST     = local.logstash_host
    LOGSTASH_PORT     = local.logstash_port
    MODEL_OBJECT_KEY  = data.aws_ssm_parameter.model_data_key.value
    MODEL_DATA_BUCKET = var.inferrer_model_data_bucket_name
  }

  subnets             = var.subnets
  max_capacity        = 5
  messages_bucket_arn = aws_s3_bucket.messages.arn
  queue_read_policy   = module.image_inferrer_queue.read_policy
  inferrer_port       = local.inferrer_port
}

data "aws_ssm_parameter" "model_data_key" {
  name = "/catalogue_pipeline/config/inferrer/model_object/${local.inferrer_model_key_config_name}"
}

resource "aws_iam_role_policy" "read_inferrer_data" {
  role   = module.image_inferrer.task_role_name
  policy = data.aws_iam_policy_document.allow_inferrer_data_access.json
}

data "aws_iam_policy_document" "allow_inferrer_data_access" {
  statement {
    actions = [
      "s3:GetObject*",
      "s3:ListBucket",
    ]

    resources = [
      "arn:aws:s3:::${var.inferrer_model_data_bucket_name}",
      "arn:aws:s3:::${var.inferrer_model_data_bucket_name}/*",
    ]
  }
}

module "image_inferrer_topic" {
  source = "../modules/topic"

  name       = "${local.namespace_hyphen}_image_inferrer"
  role_names = [module.image_inferrer.task_role_name]

  messages_bucket_arn = aws_s3_bucket.messages.arn
}

module "image_inferrer_scaling_alarm" {
  source     = "git::github.com/wellcomecollection/terraform-aws-sqs//autoscaling?ref=v1.1.2"
  queue_name = module.image_inferrer_queue.name

  queue_high_actions = [module.image_inferrer.scale_up_arn]
  queue_low_actions  = [module.image_inferrer.scale_down_arn]
}
