ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT 	= pipeline/transformer

PROJECT_ID = catalogue_pipeline

SBT_APPS = transformer_miro transformer_sierra transformer_mets transformer_calm

SBT_DOCKER_LIBRARIES    = transformer_common
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS =
LAMBDAS     =

TF_NAME =
TF_PATH =

$(val $(call stack_setup))
