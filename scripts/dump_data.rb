#!/usr/bin/ruby

require 'aws-sdk-resources'
require 'trollop'
require 'awesome_print'
require 'json'

#START MAIN
opts = Trollop::options do
  opt :region, "AWS region to talk to", :type=>:string, :default=>'eu-west-1'
  opt :environment, "Talk to CODE or PROD?", :type=>:string, :default=>'PROD'
  opt :schema, "Schema version to target", :type=>:integer, :default=>2
  opt :sortuser, "Sort by username rather than item count", :type=>:boolean, :default=>false
end

client = Aws::DynamoDB::Client.new(:region=>opts.region)

versionpart = if opts.schema>1
                "-v#{opts.schema}"
              else
                ''
              end

table_name = "LaunchDetectorUnattachedAtoms-#{opts.environment.upcase}" + "#{versionpart}"

table = Aws::DynamoDB::Table.new(table_name, :client=>client)

result = table.scan

print JSON.generate(result.items)
