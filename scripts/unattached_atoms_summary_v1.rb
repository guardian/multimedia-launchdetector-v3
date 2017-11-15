#!/usr/bin/ruby

require 'aws-sdk-resources'
require 'trollop'
require 'awesome_print'

def get_date_range(item_list, field_name)
  rtn = {
      :earliest=>nil,
      :latest=>nil
  }

  item_list.each { |item|
    rtn[:earliest] = item[field_name] if rtn[:earliest] == nil or rtn[:earliest]>item[field_name]
    rtn[:latest] = item[field_name] if rtn[:latest] == nil or rtn[:latest]<item[field_name]
  }
  rtn
end

def user_list(item_list, sortby)
  individual_users = item_list.map{|item| item['userEmail']}.uniq
  unsorted_list = individual_users.map do |username|
    {:user=>username,
     :items=>item_list.select{|item| item['userEmail']==username}.length
    }
  end
  unsorted_list.sort{|a,b| a[sortby] <=> b[sortby]}.reverse
end

#START MAIN
opts = Trollop::options do
  opt :region, "AWS region to talk to", :type=>:string, :default=>'eu-west-1'
  opt :environment, "Talk to CODE or PROD?", :type=>:string, :default=>'PROD'
  opt :csv, "Output as CSV rather than a summary", :type=>:boolean, :default=>false
  opt :sortuser, "Sort by username rather than item count", :type=>:boolean, :default=>false
end

client = Aws::DynamoDB::Client.new(:region=>opts.region)

table_name = "LaunchDetectorUnattachedAtoms-#{opts.environment.upcase}"
table = Aws::DynamoDB::Table.new(table_name, :client=>client)

result = table.scan

item_list = result.items.map{ |item|
  new_hash = Hash(item)
  new_hash['dateCreated'] = DateTime.parse(item['dateCreated'])
  new_hash['dateUpdated'] = DateTime.parse(item['dateUpdated'])
  new_hash
}

#ap item_list
created_date_range = get_date_range(item_list,"dateCreated")
created_date_range[:earliest] = created_date_range[:earliest].iso8601
created_date_range[:latest] = created_date_range[:latest].iso8601

if opts.sortuser
  sortby = :user
else
  sortby = :items
end

if opts.csv
  puts "Range start\t#{created_date_range[:earliest]}\tRange end\t#{created_date_range[:latest]}"
  puts "Total items in timespan\t#{result.count}\tIncluded items\t#{result.scanned_count}"
  puts "User\tUnattached items"

  user_list(item_list, sortby).each {|userentry|
    puts "#{userentry[:user]}\t#{userentry[:items]}"
  }
else
  puts "Got a total of #{result.count} items in the range #{created_date_range}, scanned #{result.scanned_count}"
  ap user_list(item_list, sortby)
end
