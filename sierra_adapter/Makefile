ROOT = $(shell git rev-parse --show-toplevel)
include $(ROOT)/makefiles/functions.Makefile

STACK_ROOT  = sierra_adapter

PROJECT_ID = sierra_adapter

SBT_APPS = sierra_bib_merger \
               sierra_item_merger \
               sierra_items_to_dynamo \
               sierra_reader

SBT_DOCKER_LIBRARIES    =
SBT_NO_DOCKER_LIBRARIES = sierra_adapter_common

PYTHON_APPS =
LAMBDAS     = s3_demultiplexer \
              sierra_progress_reporter \
              sierra_window_generator

TF_NAME = sierra_adapter
TF_PATH = $(STACK_ROOT)/terraform

$(val $(call stack_setup))


$(ROOT)/.docker/sierra_adapter_progress:
	echo "FROM python:3-alpine\nRUN pip3 install attrs boto3" | docker build --tag sierra_adapter_progress -
	mkdir -p $(ROOT)/.docker
	touch $(ROOT)/.docker/sierra_adapter_progress

sierra_adapter_progress: $(ROOT)/.docker/sierra_adapter_progress
	$(ROOT)/docker_run.py --aws -- \
		--volume $(ROOT)/$(STACK_ROOT):/src \
		sierra_adapter_progress \
		python /src/report_adapter_progress.py
