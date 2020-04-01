// Inferrer model data persists across pipeline versions
// and is critical for inference in the pipeline to be possible

resource "aws_s3_bucket" "inferrer-model-core-data" {
  bucket = "wellcomecollection-inferrer-model-core-data"
  acl    = "private"

  lifecycle {
    prevent_destroy = true
  }
}

