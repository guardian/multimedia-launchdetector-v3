#!/usr/bin/ruby

require 'aws-sdk-resources'
require 'trollop'
require 'awesome_print'
require 'json'

#START MAIN
opts = Trollop::options do
  opt :region, "AWS region to talk to", :type=>:string, :default=>'eu-west-1'
  opt :environment, "Talk to CODE or PROD?", :type=>:string, :default=>'CODE'
  opt :schema, "Schema version to target", :type=>:integer, :default=>2
  opt :datafile, "JSON datafile to upload", :type=>:string
  opt :addfield, "Add a new field to the data", :type=>:string
  opt :defaultvalue, "Default value for added field", :type=>:string
end

client = Aws::DynamoDB::Client.new(:region=>opts.region)

versionpart = if opts.schema>1
  "-v#{opts.schema}"
else
  ''
end

table_name = "LaunchDetectorUnattachedAtoms-#{opts.environment.upcase}" + "#{versionpart}"

puts "Writing to #{table_name}"

incoming_data = open(opts.datafile) do |f|
   JSON.load(f.read)
end

if opts.addfield
  processed_data = incoming_data.map do |e|
    e[opts.addfield] = opts.defaultvalue
    e
  end
else
  processed_data = incoming_data
end

request_list = processed_data.map do |e|
  {:put_request=>{:item=>e}}
end

ap request_list

request_list.each_slice(25) do |data|
  client.batch_write_item({
      :request_items=>{
          table_name=>data
      }
  })
  puts "Wrote a batch of 25 items..."
end

