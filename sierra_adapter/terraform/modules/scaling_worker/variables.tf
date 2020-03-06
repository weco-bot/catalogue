variable "service_name" {
}

variable "container_image" {
}

variable "env_vars" {
  type = map(string)
}

variable "secret_env_vars" {
  type    = map(string)
  default = {}
}

variable "cpu" {
}

variable "memory" {
}

variable "min_capacity" {
}

variable "max_capacity" {
}

variable "desired_task_count" {
  default = 1
}

variable "namespace_id" {
}

variable "cluster_name" {
}

variable "cluster_arn" {
}

variable "launch_type" {
  default = "FARGATE"
}

variable "subnets" {
  type = list(string)
}

variable "security_group_ids" {
  type    = list(string)
  default = []
}

variable "use_fargate_spot" {
  type    = bool
  default = false
}
