#!/usr/bin/python

####
# 02/2006 Will Holcomb <wholcomb@gmail.com>
# 
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
# 
# This library is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# 7/26/07 Slightly modified by Brian Schneider  
# in order to support unicode files ( multipart_encode function )
#
# 3/23/10 Slightly modified by Spencer Lord  
# in order to print the chunked transfer encoded response
# in order to change the main (sample) to do speech recognition.
"""
Usage:
  Enables the use of multipart/form-data for posting forms

Inspirations:
  Upload files in python:
    http://aspn.activestate.com/ASPN/Cookbook/Python/Recipe/146306
  urllib2_file:
    Fabien Seisen: <fabien@seisen.org>

Example:
  import MultipartPostHandler, urllib2, cookielib

  cookies = cookielib.CookieJar()
  opener = urllib2.build_opener(urllib2.HTTPCookieProcessor(cookies),
                                MultipartPostHandler.MultipartPostHandler)
  params = { "username" : "bob", "password" : "riviera",
             "file" : open("filename", "rb") }
  opener.open("http://wwww.bobsite.com/upload/", params)

Further Example:
  The main function of this file is a sample which ...
  
"""

import urllib
import urllib2
import mimetools, mimetypes
import os, stat
from cStringIO import StringIO

class Callable:
    def __init__(self, anycallable):
        self.__call__ = anycallable

# Controls how sequences are uncoded. If true, elements may be given multiple values by
#  assigning a sequence.
doseq = 1

class MultipartPostHandler(urllib2.BaseHandler):
    handler_order = urllib2.HTTPHandler.handler_order - 10 # needs to run first

    def http_request(self, request):
        data = request.get_data()
        if data is not None and type(data) != str:
            v_files = []
            v_vars = []
            try:
                 for(key, value) in data.items():
                     if type(value) == file:
                         v_files.append((key, value))
                     else:
                         v_vars.append((key, value))
            except TypeError:
                systype, value, traceback = sys.exc_info()
                raise TypeError, "not a valid non-string sequence or mapping object", traceback

            if len(v_files) == 0:
                data = urllib.urlencode(v_vars, doseq)
            else:
                boundary, data = self.multipart_encode(v_vars, v_files)

                contenttype = 'multipart/form-data; boundary=%s' % boundary
                if(request.has_header('Content-Type')
                   and request.get_header('Content-Type').find('multipart/form-data') != 0):
                    print "Replacing %s with %s" % (request.get_header('content-type'), 'multipart/form-data')
                request.add_unredirected_header('Content-Type', contenttype)

            request.add_data(data)
        
        return request

    def multipart_encode(vars, files, boundary = None, buf = None):
        if boundary is None:
            boundary = mimetools.choose_boundary()
        if buf is None:
            buf = StringIO()
        for(key, value) in vars:
            buf.write('--%s\r\n' % boundary)
            buf.write('Content-Disposition: form-data; name="%s"' % key)
            buf.write('\r\n\r\n' + value + '\r\n')
        for(key, fd) in files:
            file_size = os.fstat(fd.fileno())[stat.ST_SIZE]
            filename = fd.name.split('/')[-1]
            contenttype = mimetypes.guess_type(filename)[0] or 'application/octet-stream'
            buf.write('--%s\r\n' % boundary)
            buf.write('Content-Disposition: form-data; name="%s"; filename="%s"\r\n' % (key, filename))
            buf.write('Content-Type: %s\r\n' % contenttype)
            # buffer += 'Content-Length: %s\r\n' % file_size
            fd.seek(0)
            buf.write('\r\n' + fd.read() + '\r\n')
        buf.write('--' + boundary + '--\r\n\r\n')
        buf = buf.getvalue()
        return boundary, buf
    multipart_encode = Callable(multipart_encode)

    https_request = http_request


def main():
    import tempfile, sys, getopt

    try:
	    opts, args = getopt.getopt(sys.argv[1:], "hg:cnls:", ["help", "grammar=","continuous","noend","live","service="])
    except getopt.GetoptError, err:
        # print help information and exit:
        print str(err) # will print something like "option -a not recognized"
        usage()
        sys.exit(2)
    cflag="false"
    lm="true"
    ep = "true"
    batch="true"
    input = 'C:/work/speechcloud/etc/prompts/fire_and_ice_frost_add_64kb.mp3'
    #input = 'C:/work/speechcloud/etc/prompts/gtd.wav'
    service = "http://spokentech.net/speechcloud/SpeechUploadServlet"
    grammar = None
    for o, a in opts:
        if o in ("-c", "--continuous"):
            cflag = "true"
        elif o in ("-n", "--noend"):
            ep = "true"
        elif o in ("-l", "--live"):
            batch = False
        elif o in ("-h", "--help"):
            usage()
            sys.exit()
        elif o in ("-s", "--service"):
            service = "http://"+a+"/speechcloud/SpeechUploadServlet"
        elif o in ("-g", "--grammar"):
            lm = "false"
            grammar = a
        else:
	    print o
            assert False, "unhandled option"

    for arg in args:
        print arg
	output = arg


    opener = urllib2.build_opener(MultipartPostHandler)
    #params = { "lmFlag" : "true", "continuousFlag" : "true","doEndpointing" : "true", "CmnBatchFlag" : "true",
             #"audio" : open("C:/work/speechcloud/etc/prompts/gtd.wav", "rb") }
    if lm:
	    params = {"lmFlag" : lm, "continuousFlag" : cflag,"doEndpointing" : ep, "CmnBatchFlag" : batch, "audio" : open(input, "rb") }
    else:
        params = { "lmFlag" : lm, "continuousFlag" : cflag,"doEndpointing" : ep, "CmnBatchFlag" : batch, "audio" : open(input, "rb"),"grammar" : open(grammar, "rb")  }
    #response = opener.open("http://localhost:8090/speechcloud/SpeechUploadServlet", params)
    print service
    response = opener.open(service, params)

    bytes_so_far = 0
    while 1:
      chunk = response.read()
      bytes_so_far += len(chunk)

      if not chunk:
         break
      print chunk

if __name__=="__main__":
    main()

