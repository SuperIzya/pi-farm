#!/usr/bin/env bash
# Configure git to use the project's shared hooks directory.
# Run once after cloning: bash setup.sh

git config core.hooksPath .githooks
echo "Git hooks configured: .githooks"
