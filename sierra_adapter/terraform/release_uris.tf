locals {
  sierra_bib_merger      = "${module.images.services["sierra_bib_merger"]}"
  sierra_item_merger     = "${module.images.services["sierra_item_merger"]}"
  sierra_items_to_dynamo = "${module.images.services["sierra_items_to_dynamo"]}"
  sierra_reader          = "${module.images.services["sierra_reader"]}"
}

module "images" {
  source = "git::https://github.com/wellcometrust/terraform.git//ecs/modules/images?ref=v19.8.0"

  project = "sierra_adapter"
  label   = "latest"

  services = [
    "sierra_bib_merger",
    "sierra_item_merger",
    "sierra_items_to_dynamo",
    "sierra_reader",
  ]
}
