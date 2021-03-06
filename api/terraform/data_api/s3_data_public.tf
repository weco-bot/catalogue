locals {
  public_data_bucket_name = "wellcomecollection-data-public-delta"
}

resource "aws_s3_bucket" "public_data" {
  bucket = local.public_data_bucket_name
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }

  policy = data.aws_iam_policy_document.public_data_bucket_get_access_policy.json
}

data "aws_iam_policy_document" "public_data_bucket_get_access_policy" {
  statement {
    actions = [
      "s3:GetObject",
    ]

    principals {
      identifiers = ["*"]
      type        = "AWS"
    }

    resources = [
      "arn:aws:s3:::${local.public_data_bucket_name}/*",
    ]
  }
}

# This file is served from the root of data.wellcomecollection.org.
resource "aws_s3_bucket_object" "index_page" {
  bucket  = aws_s3_bucket.public_data.id
  key     = "index.html"
  content = file("${path.module}/data_wc_index.html")
  etag    = md5(file("${path.module}/data_wc_index.html"))

  content_type = "text/html"
}
