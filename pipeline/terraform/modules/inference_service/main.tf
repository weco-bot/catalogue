module "service" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//service?ref=v1.2.0"

  service_name        = var.service_name
  cluster_arn         = var.cluster_arn
  task_definition_arn = aws_ecs_task_definition.task.arn
  subnets             = var.subnets
  namespace_id        = var.namespace_id
  security_group_ids  = var.security_group_ids
  launch_type         = var.launch_type
  desired_task_count  = var.desired_task_count
}

module "scaling" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//autoscaling?ref=v1.2.0"

  name = var.service_name

  cluster_name = var.cluster_name
  service_name = var.service_name

  min_capacity = var.min_capacity
  max_capacity = var.max_capacity
}
