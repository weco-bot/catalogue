module "staging_routing" {
  source          = "../modules/routing"
  environment     = "staging"
  api_id          = aws_api_gateway_rest_api.catalogue_api.id
  listener_port   = 1235
  domain_name     = "catalogue.api-stage.wellcomecollection.org"
  certificate_arn = aws_acm_certificate_validation.catalogue_api_validation.certificate_arn
  aws_region      = "eu-west-1"

  providers = {
    aws.dns = aws.dns
  }
}

module "prod_routing" {
  source          = "../modules/routing"
  environment     = "prod"
  api_id          = aws_api_gateway_rest_api.catalogue_api.id
  listener_port   = 8081
  domain_name     = "catalogue.api.wellcomecollection.org"
  certificate_arn = aws_acm_certificate_validation.catalogue_api_validation.certificate_arn
  aws_region      = "eu-west-1"

  providers = {
    aws.dns = aws.dns
  }
}
