provider "aws" {
  region  = "eu-west-1"
  version = "~> 2.7"

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-developer"
  }
}

provider "aws" {
  alias = "platform"

  region  = "eu-west-1"
  version = "~> 2.7"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
  }
}

provider "aws" {
  alias = "routemaster"

  region  = "eu-west-1"
  version = "~> 2.7"

  assume_role {
    role_arn = "arn:aws:iam::250790015188:role/wellcomecollection-assume_role_hosted_zone_update"
  }
}