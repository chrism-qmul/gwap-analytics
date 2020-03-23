#!/bin/bash
curl -XPOST localhost:3000/upload -F file=@$1 -Fgame=$2
