ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT  = snapshots

PROJECT_ID = catalogue_api

SBT_APPS = snapshot_generator

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES =

PYTHON_APPS =
LAMBDAS     = snapshot_scheduler snapshot_slack_alarms

TF_NAME =
TF_PATH =

$(val $(call stack_setup))
