#!/usr/bin/env python
# -*- encoding: utf-8

import os
import subprocess
import sys

from git_utils import get_changed_paths, git
from sbt_dependency_tree import Repository


def check_call(cmd):
    """
    A wrapped version of subprocess.check_call() that doesn't print a
    traceback if the command errors.
    """
    print("*** Running %r" % " ".join(cmd))
    try:
        return subprocess.check_call(cmd)
    except subprocess.CalledProcessError as err:
        print(err)
        sys.exit(err.returncode)


def make(*args):
    """Run a Make command, and check it completes successfully."""
    check_call(["make"] + list(args))


def should_run_sbt_project(project_name, changed_paths):
    repo = Repository(".sbt_metadata")
    project = repo.get_project(project_name)

    interesting_paths = [p for p in changed_paths if not p.startswith(".sbt_metadata")]

    if ".travis.yml" in interesting_paths:
        return True
    if "build.sbt" in interesting_paths:
        return True
    if any(p.startswith("project/") for p in interesting_paths):
        return True

    for path in interesting_paths:
        if path.startswith((".travis", "docs/")):
            continue

        if path.endswith(".tf"):
            continue

        if path.endswith("Makefile"):
            return os.path.dirname(project.folder) == os.path.dirname(path)

        try:
            project_for_path = repo.lookup_path(path)
        except KeyError:
            # This path isn't associated with a project!
            return True
        else:
            if project.depends_on(project_for_path):
                return True

    return False


if __name__ == "__main__":

    travis_event_type = os.environ["TRAVIS_EVENT_TYPE"]
    travis_build_stage = os.environ["TRAVIS_BUILD_STAGE_NAME"]

    try:
        # If it's not an sbt task, we always run it no matter what.
        task = os.environ["TASK"]
    except KeyError:
        sbt_project_name = os.environ["SBT_PROJECT"]

        if travis_event_type == "pull_request":
            changed_paths = get_changed_paths("HEAD", "master")
        else:
            git("fetch", "origin")
            changed_paths = get_changed_paths(os.environ["TRAVIS_COMMIT_RANGE"])

        if should_run_sbt_project(sbt_project_name, changed_paths=changed_paths):
            task = "%s-test"
        else:
            print("Nothing in this patch affects %s, so skipping tests")
            sys.exit(0)

    make(task)

    if travis_event_type == "push" and travis_build_stage == "Services":
        make(task.replace("test", "publish"))
