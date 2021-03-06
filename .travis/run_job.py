#!/usr/bin/env python
# -*- encoding: utf-8

import argparse
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


def should_run_sbt_project(repo, project_name, changed_paths):
    project = repo.get_project(project_name)

    interesting_paths = [p for p in changed_paths if not p.startswith(".sbt_metadata")]

    if ".travis.yml" in interesting_paths:
        print("*** Relevant: .travis.yml")
        return True
    if "build.sbt" in interesting_paths:
        print("*** Relevant: build.sbt")
        return True
    if any(p.startswith("project/") for p in interesting_paths):
        print("*** Relevant: project/")
        return True

    for path in interesting_paths:
        if path.startswith((".travis", "docs/")):
            continue

        if path.endswith(".tf"):
            continue

        if path.endswith("Makefile"):
            if os.path.dirname(project.folder) == os.path.dirname(path):
                print("*** %s is defined by %s" % (project.name, path))
                return True
            else:
                continue

        try:
            project_for_path = repo.lookup_path(path)
        except KeyError:
            # This path isn't associated with a project!
            print("*** Unrecognised path: %s" % path)
            return True
        else:
            if project.depends_on(project_for_path):
                print("*** %s depends on %s" % (project.name, project_for_path.name))
                return True

        print("*** Not significant: %s" % path)

    return False


if __name__ == "__main__":

    travis_event_type = os.environ["TRAVIS_EVENT_TYPE"]
    travis_build_stage = os.environ["TRAVIS_BUILD_STAGE_NAME"]
    travis_commit_range = os.environ["TRAVIS_COMMIT_RANGE"]

    try:
        # If it's not an sbt task, we always run it no matter what.
        task = os.environ["TASK"]
    except KeyError:
        parser = argparse.ArgumentParser()
        parser.add_argument("project_name", default=os.environ.get("SBT_PROJECT"))
        parser.add_argument("--changes-in", nargs="*")
        args = parser.parse_args()

        task = f"{args.project_name}-test"

        if args.changes_in:
            change_globs = args.changes_in + [".travis.yml"]
        else:
            change_globs = None

        if travis_event_type == "pull_request":
            changed_paths = get_changed_paths("HEAD", "master", globs=change_globs)
        else:
            git("fetch", "origin")
            changed_paths = get_changed_paths(travis_commit_range, globs=change_globs)

        sbt_repo = Repository(".sbt_metadata")
        try:
            if not should_run_sbt_project(sbt_repo, args.project_name, changed_paths):
                print(
                    f"Nothing in this patch affects {args.project_name}, so skipping tests"
                )
                sys.exit(0)
        except (FileNotFoundError, KeyError):
            if args.changes_in and not changed_paths:
                print(
                    f"Nothing in this patch affects the files {args.changes_in}, so skipping tests"
                )
                sys.exit(0)

    make(task)
    if travis_event_type == "push" and travis_build_stage.lower() == "services":
        make(task.replace("test", "publish"))
