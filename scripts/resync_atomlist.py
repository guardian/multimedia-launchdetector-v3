#!/usr/bin/python

import hashlib
import hmac
from optparse import OptionParser
from datetime import datetime
import base64
from email.utils import formatdate
import requests
from time import mktime
from urlparse import urlparse
from pprint import pprint
import time

class HttpError(StandardError):
    def __init__(self, uri, code, content, sent_headers, response_headers):
        self.code = code
        self.uri = uri
        self.content = content
        self.request_headers = sent_headers
        self.response_headers = response_headers

    def __str__(self):
        return "HTTP error {code} accessing {uri}".format(code=self.code, uri=self.uri)


def get_token(uri, secret):
    httpdate = formatdate(timeval=mktime(datetime.now().timetuple()),localtime=False,usegmt=True)
    url_parts = urlparse(uri)

    string_to_sign = "{0}\n{1}".format(httpdate, url_parts.path)
    print "string_to_sign: " + string_to_sign
    hm = hmac.new(secret, string_to_sign,hashlib.sha256)
    return "HMAC {0}".format(base64.b64encode(hm.digest())), httpdate


def request_atom_resend(atomid, options):
    """
    Message the media atom tool to request that the atom is resynced
    :param options:
    :return:
    """
    uri = "https://{host}/api2/pluto/resend/{id}".format(host=options.host, id=atomid)
    print "uri is " + uri
    authtoken, httpdate = get_token(uri, options.secret)
    print authtoken

    headers = {
        'X-Gu-Tools-HMAC-Date': httpdate,
        'X-Gu-Tools-HMAC-Token': authtoken
    }

    print headers
    response = requests.post(uri,headers=headers)
    print "Server returned {0}".format(response.status_code)
    pprint(response.headers)
    if response.status_code==200:
        pprint(response.json())
    else:
        raise HttpError(uri,response.status_code, response.text, headers, response.headers)


def request_metadata_resync(atomid, options):
    """
    Message the launch detector to request that metadata is updated
    :param atomid:
    :param options:
    :return:
    """
    uri = "http://{host}/update/{id}".format(host=options.ldhost, id=atomid)
    response = requests.put(uri)
    print "Server returned {0}".format(response.status_code)
    if response.status_code==200:
        try:
            pprint(response.json())
        except ValueError:
            print response.text
    else:
        raise HttpError(uri,response.status_code, response.text, {}, response.headers)

#START MAIN
parser = OptionParser()
parser.add_option("--host", dest="host", help="host to access", default="video.local.dev-gutools.co.uk")
parser.add_option("--launchdetector", dest="ldhost", help="host to access launch detector on", default="localhost")
parser.add_option("-l", "--atomlist", dest="atomlist", help="text file containing a list of atom ids")
parser.add_option("-s", "--secret", dest="secret", help="shared secret to use")
(options, args) = parser.parse_args()

if options.secret is None:
    print "You must supply the password in --secret"
    exit(1)

with open(options.atomlist,"r") as f:
    content = f.read()
    atomlist = content.split("\n")
    resync_list = []

    while len(atomlist)>0:
        for atomid in atomlist:
            try:
                print "---------------------"
                print "Requesting re-sync of {0}".format(atomid)
                request_metadata_resync(atomid,options)
                atomlist.remove(atomid)
            except HttpError as e:
                print str(e)
                print e.content
                continue
        if len(resync_list)>0:
            print "Waiting 10 seconds before retrying resync"
            time.sleep(10)

    print "All done"