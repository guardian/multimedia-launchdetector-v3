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
end

client = Aws::DynamoDB::Client.new(:region=>opts.region)
puts "a"

versionpart = if opts.schema>1
  "-v#{opts.schema}"
else
  ''
end

puts versionpart

table_name = "LaunchDetectorUnattachedAtoms-#{opts.environment.upcase}" + "#{versionpart}"

puts "Writing to #{table_name}"

incoming_data = open(opts.datafile) do |f|
   JSON.load(f.read)
end

request_list = incoming_data.map do |e|
  {:put_request=>{:item=>e}}
end

#ap request_list

request_list.each_slice(25) do |data|
  client.batch_write_item({
      :request_items=>{
          table_name=>data
      }
  })
  puts "Wrote a batch of 25 items..."
end

