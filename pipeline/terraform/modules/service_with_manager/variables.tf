variable "subnets" {
  type = list(string)
}

variable "namespace_id" {
}

variable "cluster_name" {
}

variable "cluster_arn" {
}

variable "service_name" {
}

variable "desired_task_count" {
  type    = number
  default = 1
}

variable "launch_type" {
  default = "FARGATE"
}

variable "security_group_ids" {
  type    = list(string)
  default = []
}

variable "host_cpu" {
  type    = number
  default = 512
}

variable "host_memory" {
  type    = number
  default = 1024
}

variable "max_capacity" {
  type = number
}

variable "min_capacity" {
  type    = number
  default = 0
}

variable "messages_bucket_arn" {}

variable "queue_read_policy" {}

variable "app_container_image" {}

variable "app_container_port" {
  type    = number
  default = 80
}

variable "app_container_name" {}

variable "app_env_vars" {
  type = map(string)
}

variable "secret_app_env_vars" {
  type    = map(string)
  default = {}
}

variable "app_cpu" {
  type    = number
  default = 512
}

variable "app_memory" {
  type    = number
  default = 1024
}

variable "app_healthcheck_json" {
  type    = string
  default = ""
}

variable "manager_container_image" {}

variable "manager_container_name" {}

variable "manager_env_vars" {
  type = map(string)
}

variable "secret_manager_env_vars" {
  type    = map(string)
  default = {}
}

variable "manager_cpu" {
  type    = number
  default = 512
}

variable "manager_memory" {
  type    = number
  default = 1024
}
