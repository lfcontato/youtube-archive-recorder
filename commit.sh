#!/bin/bash

current_date=$(date '+%Y-%m-%d-%H-%M-%S')
computer_name=$(hostname)

git add .
git commit -m "$computer_name $current_date"
git push
