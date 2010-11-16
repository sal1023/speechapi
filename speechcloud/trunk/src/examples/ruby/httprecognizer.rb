#
# Copyright (C) 2010 Spokentech - http://www.spokentech.com
#
# This library is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 2.1 of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
#
# Contact: spencer@spokentech.com
#

require 'httpclient'
require 'uri'

if __FILE__ == $0

   uri = URI.parse('http://localhost:8090/speechcloud/SpeechUploadServlet') 
   clnt = HTTPClient.new

   File.open('C:/tmp/fire_and_ice_frost_add_64kb.mp3') do |file|
      body = { 'lmFlag'=>'true', 'continuousFlag' => 'true', 'doEndpointing' => 'true' ,'CmnBatchFlag' => 'true', 'audio' => file  }
      res = clnt.post(uri, body) do |chunk|
         puts chunk
      end
   end
	

end
