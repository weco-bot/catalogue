output "task_role_name" {
  value = module.task_role.name
}

output "scale_up_arn" {
  value = module.scaling.scale_up_arn
}

output "scale_down_arn" {
  value = module.scaling.scale_down_arn
}
