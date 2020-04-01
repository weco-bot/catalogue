resource "aws_ecs_task_definition" "task" {
  family = var.service_name
  container_definitions = templatefile("container_definitions.json.tpl", {
    inference_manager_container_name          = "inference_manager",
    inference_manager_environment_vars        = module.inference_manager_env_vars.env_vars_string,
    inference_manager_secret_environment_vars = module.inference_manager_secrets.env_vars_string,
    inference_manager_container_image         = var.inference_manager_container_image,
    inference_manager_memory                  = var.inference_manager_memory,
    inference_manager_cpu                     = var.inference_manager_cpu,

    inferrer_container_name          = "inferrer",
    inferrer_environment_vars        = module.inferrer_env_vars.env_vars_string,
    inferrer_secret_environment_vars = module.inferrer_secrets.env_vars_string,
    inferrer_container_image         = var.inferrer_container_image,
    inferrer_memory                  = var.inferrer_memory,
    inferrer_cpu                     = var.inferrer_cpu,
    inferrer_port_mappings = jsonencode([
      { containerPort = var.inferrer_port }
    ]),
  })

  task_role_arn      = module.task_role.task_role_arn
  execution_role_arn = module.task_role.task_execution_role_arn

  network_mode = "awsvpc"

  requires_compatibilities = [var.launch_type]

  cpu    = var.host_cpu
  memory = var.host_memory
}

module "task_role" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/modules/iam_role?ref=v1.5.2"

  task_name = var.service_name
}

module "inference_manager_env_vars" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/modules/env_vars?ref=v1.5.2"

  env_vars = var.inference_manager_env_vars
}

module "inference_manager_secrets" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/modules/secrets?ref=v1.5.2"

  secret_env_vars     = var.inference_manager_secret_env_vars
  execution_role_name = module.task_role.task_execution_role_name
}

module "inferrer_env_vars" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/modules/env_vars?ref=v1.5.2"

  env_vars = var.inferrer_env_vars
}

module "inferrer_secrets" {
  source = "git::github.com/wellcomecollection/terraform-aws-ecs-service.git//task_definition/modules/secrets?ref=v1.5.2"

  secret_env_vars     = var.inferrer_secret_env_vars
  execution_role_name = module.task_role.task_execution_role_name
}
