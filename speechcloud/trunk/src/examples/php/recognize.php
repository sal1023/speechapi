<?php
/*
* Copyright (C) 2010 Spokentech - http://www.spokentech.com
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*
* Contact: spencer@spokentech.com
*
*/

	
$lmFlag='true';
$continuousFlag='true';
$doEndpointing='true';
$CmnBatchFlag='true';
$encoding='PCM_SIGNED';
$sampleRate='8000';
$bigEndian='false';
$bytesPerValue='2';
//$fullfilepath = 'C:\work\speechcloud\etc\prompts\gtd.wav';
$fullfilepath = 'C:\tmp\fire_and_ice_frost_add_64kb.mp3';
$upload_url = 'http://spokentech.net/speechcloud/SpeechUploadServlet';
$params = array(
  'lmFlag'=>$lmFlag,
  'continuousFlag'=>$continuousFlag ,
  'doEndpointing'=>$doEndpointing ,
  'CmnBatchFlag'=>$CmnBatchFlag ,
  //'encoding'=>$encoding ,
  //'sampleRate'=>$sampleRate ,
  //'bigEndian'=>$bigEndian ,
  //'bytesPerValue'=>$bytesPerValue,
  'audio'=>"@$fullfilepath"
);       
set_time_limit(0); 
$ch = curl_init();
curl_setopt($ch, CURLOPT_VERBOSE, 1);
curl_setopt($ch, CURLOPT_TIMEOUT, 300);
curl_setopt($ch, CURLOPT_CONNECTTIMEOUT, 300);
curl_setopt($ch, CURLOPT_URL, $upload_url);
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, $params);
$response = curl_exec($ch);
echo "$response";
curl_close($ch);
?>
