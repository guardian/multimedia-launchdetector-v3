#!/usr/bin/env ruby

STDIN.readlines.each do |entry|
  puts("<field>\n    <name>#{entry.chomp}</name>\n    <value/>\n</field>\n")
end