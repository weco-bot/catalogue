import click

from src.elastic import get_random_feature_vectors
from src.lsh import get_object_for_storage
from src.storage import store_model


@click.command()
@click.option(
    "-n", help="number of groups to split the feature vectors into", default=256
)
@click.option(
    "-m", help="number of clusters to find within each feature group", default=32
)
@click.option(
    "--sample_size", help="number of embeddings to train clusters on", default=25000
)
@click.option(
    "--bucket-name",
    help="Name of the S3 bucket in which model data is stored",
    envvar="MODEL_DATA_BUCKET"
)
def main(n, m, sample_size, bucket_name):
    feature_vectors = get_random_feature_vectors(sample_size)

    model = get_object_for_storage(feature_vectors, m, n)
    store_model(bucket_name=bucket_name, **model)


if __name__ == "__main__":
    main()