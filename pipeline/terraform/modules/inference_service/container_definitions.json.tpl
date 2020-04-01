[
  {
    "essential": true,
    "name": "${inference_manager_container_name}",
    "environment": ${inference_manager_environment_vars},
    "secrets": ${inference_manager_secret_environment_vars},
    "image": "${inference_manager_container_image}",
    "memory": ${inference_manager_memory},
    "cpu": ${inference_manager_cpu},
    "portMappings": ${inference_manager_port_mappings},
    "user": "root",
  },
  {
    "essential": false,
    "name": "${inferrer_container_name}",
    "environment": ${inferrer_environment_vars},
    "secrets": ${inferrer_secret_environment_vars},
    "image": "${inferrer_container_image}",
    "memory": ${inferrer_memory},
    "cpu": ${inferrer_cpu},
    "portMappings": ${inferrer_port_mappings},
    "user": "root",
  }
]
