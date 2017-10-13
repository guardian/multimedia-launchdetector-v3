#!/usr/bin/env bash

#
#Stack name:
#    launchdetector-CODE
#
#AmiId
#    ami-785db401
#
#App
#    launchdetector
#
#CapiStreamArn
#    arn:aws:kinesis:eu-west-1:308506855511:stream/content-api-firehose-v2-PROD
#
#CapiStreamName
#    content-api-firehose-v2-PROD
#
#DeploySubnets
#    subnet-aa1a56ce,subnet-ca9f1192,subnet-f6f78d80
#
#InstanceType
#    t2.nano
#
#KeyPair
#    AndyTestKey
#
#Mode
#    preview
#
#OfficeIpRange
#    10.0.0.0/8
#
#Stage
#    CODE
#
#VPCID
#    vpc-2288e346
aws cloudformation create-stack --profile multimedia --region eu-west-1 --stack-name launchdetector-CODE2 \
  --template-body file://launchdetector.yaml \
  --capabilities CAPABILITY_IAM \
  --parameters ParameterKey=CapiStreamArn,ParameterValue=arn:aws:kinesis:eu-west-1:308506855511:stream/content-api-firehose-v2-PROD \
ParameterKey=CapiStreamName,ParameterValue=content-api-firehose-v2-PROD \
ParameterKey=App,ParameterValue=launchdetector \
ParameterKey=Stack,ParameterValue=multimedia \
ParameterKey=Stage,ParameterValue=CODE \
ParameterKey=OfficeIpRange,ParameterValue=10.0.0.0/8 \
ParameterKey=VPCID,ParameterValue=vpc-2288e346 \
ParameterKey=Mode,ParameterValue=preview \
ParameterKey=KeyPair,ParameterValue=AndyTestKey \
ParameterKey=InstanceType,ParameterValue=t2.nano \
ParameterKey=DeploySubnets,ParameterValue=\"subnet-aa1a56ce,subnet-ca9f1192,subnet-f6f78d80\" \
ParameterKey=AmiId,ParameterValue=ami-785db401