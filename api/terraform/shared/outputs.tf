output "catalogue_api_nlb_arn" {
  value = "${module.nlb.arn}"
}

output "cluster_name" {
  value = "${aws_ecs_cluster.catalogue_api.name}"
}

output "nlb_arn" {
  value = "${module.nlb.arn}"
}

output "api_gateway_id" {
  value = "${aws_api_gateway_rest_api.api.id}"
}

output "api_gateway_name" {
  value = "${aws_api_gateway_rest_api.api.name}"
}

output "certificate_arn" {
  value = "${aws_acm_certificate_validation.catalogue_api_validation.certificate_arn}"
}

output "interservice_security_group_id" {
  value = "${aws_security_group.interservice.id}"
}

output "service_lb_ingress_security_group_id" {
  value = "${aws_security_group.service_lb_ingress_security_group.id}"
}

output "logstash_transit_service_name" {
  value = "${local.logstash_transit_service_name}"
}