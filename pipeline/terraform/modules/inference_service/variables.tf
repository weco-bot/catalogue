variable "inference_manager_env_vars" {
  type = map(string)
}

variable "inference_manager_secret_env_vars" {
  type    = map(string)
  default = {}
}

variable "inferrer_env_vars" {
  type = map(string)
}

variable "inferrer_secret_env_vars" {
  type    = map(string)
  default = {}
}

variable "subnets" {
  type = list(string)
}

variable "inferrer_container_image" {}

variable "inference_manager_container_image" {}

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

variable "inference_manager_cpu" {
  type    = number
  default = 512
}

variable "inference_manager_memory" {
  type    = number
  default = 1024
}

variable "inferrer_cpu" {
  type    = number
  default = 512
}

variable "inferrer_memory" {
  type    = number
  default = 1024
}

variable "inferrer_port" {
  type    = number
  default = 80
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
