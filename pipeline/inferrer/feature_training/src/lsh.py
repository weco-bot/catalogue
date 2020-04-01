import pickle
import numpy as np
from datetime import datetime
from sklearn.cluster import KMeans
from tqdm import tqdm


def split_features(feature_vectors, n_groups):
    feature_groups = np.split(feature_vectors, indices_or_sections=n_groups, axis=1)
    return feature_groups


def train_clusters(feature_group, m):
    clustering_alg = KMeans(n_clusters=m).fit(feature_group)
    return clustering_alg


def get_object_for_storage(feature_vectors, m, n, verbose=False):
    print("Generating binary of model...")
    feature_groups = split_features(feature_vectors, n)
    model_list = [
        train_clusters(feature_group, m)
        for feature_group in (tqdm(feature_groups) if verbose else feature_groups)
    ]

    return {
        "object_binary": pickle.dumps(model_list),
        "name": datetime.now().strftime("%Y-%m-%d"),
        "prefix": "lsh_model"
    }
