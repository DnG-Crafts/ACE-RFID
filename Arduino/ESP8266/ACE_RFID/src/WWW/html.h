static const char indexData[] PROGMEM = R"==(<!DOCTYPE html>
<html>
<head>
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>ACE RFID</title>
  <style>
    .btn {
      background-color: #0047AB;
      border: none;
      color: #FFFFFF;
      padding: 16px 30px 16px 30px;
      font-size: 16px;
      cursor: pointer;
      font-weight: bold;
      margin: 10px;
      border-radius: 30px;
    }

    .btn:hover {
      background-color: #1976D2;
    }

    svg {
      fill: #0047AB;
      background-color: #E0E0E0;
      border: none;
      cursor: pointer;
    }

    svg:hover {
      fill: #1976D2;
      background-color: #E0E0E0;
    }

    body {
      font-family: arial, sans-serif;
      background-color: #E0E0E0;
      margin: 0;
    }

    label {
      color: #000000;
      font-size: 18px;
      font-weight: bold;
      font-family: arial, sans-serif;
    }

    .main {
      padding: 0;
      position: absolute;
      top: 0;
      right: 0;
      bottom: 0;
      left: 0;
      background-color: #E0E0E0;
      font-size: 16px;
      font-weight: bold;
      color: #000000;
      font-family: arial, sans-serif;
    }

    input[type="color"] {
      border: 1px solid #000000;
      background-color: #ffffff;
      width: 140px;
      height: 40px;
      margin: 10px;
      cursor: pointer;
    }

    table {
      font-family: arial, sans-serif;
      border-collapse: collapse;
    }

    select {
      height: 40px;
      width: 140px;
      padding: 10px;
      font-size: 16px;
      margin: 10px;
      cursor: pointer;
    }

    .msg {
      color: #000000;
      font-size: 16px;
      font-weight: bold;
      text-shadow: none;
    }

    .errormsg {
      color: #ff0000;
      font-size: 16px;
      font-weight: bold;
      text-shadow: none;
    }

    dialog {
      padding: 0 !important;
      border: none;
      margin: 0;
      min-width: 100%;
      min-height: 100%;
      max-width: 100%;
      max-height: 100%;
      background-color: #E0E0E0;
    }

    td {
      border: none;
      text-align: left;
      padding: 8px;
    }

    th {
      border: none;
      color: white;
      background-color: #154c79;
      text-align: center;
      padding: 8px;
    }

    input[type="text"] {
      padding: 10px;
      font-size: 16px;
      margin: 2px;
      border: 1px solid #000000;
      cursor: pointer;
    }

    input[type="text"]:invalid {
      border: 1px solid red;
    }

    input[type="text"]:invalid:focus {
      background-color: rgba(255, 0, 0, 0.486);
      background-blend-mode: overlay;
    }

    progress {
      border-radius: 6px;
      width: 200px;
      height: 16px;
      overflow: hidden;
    }

    progress::-webkit-progress-bar {
      background-color: #ffffff;
      border-radius: 6px;
    }

    progress::-webkit-progress-value {
      background-color: #3498db;
      border-bottom-left-radius: 6px;
      border-top-left-radius: 6px;
      float: left;
    }

    #loader {
      z-index: 1;
      width: 50px;
      height: 50px;
      margin: 0 0 0 0;
      border: 6px solid #f3f3f3;
      border-radius: 50%;
      border-top: 6px solid #3498db;
      width: 50px;
      height: 50px;
      -webkit-animation: spin 2s linear infinite;
      animation: spin 2s linear infinite;
    }

    @-webkit-keyframes spin {
      0% {
        -webkit-transform: rotate(0deg);
      }

      100% {
        -webkit-transform: rotate(360deg);
      }
    }

    @keyframes spin {
      0% {
        transform: rotate(0deg);
      }

      100% {
        transform: rotate(360deg);
      }
    }
  </style>
  <script>
    function sendData() {
      document.getElementById("message").innerHTML = "<label class=\"msg\">Saving spool settings...</label>";
      var materialType = document.getElementById("materialType").value;
      var materialWeight = document.getElementById("materialWeight").value;
      var materialColor = document.getElementById("materialColor").value;
      var postdata = "materialType=" + materialType + "&materialWeight=" + materialWeight + "&materialColor=" + materialColor;
      localStorage.setItem("materialType", materialType);
      localStorage.setItem("materialWeight", materialWeight);
      localStorage.setItem("materialColor", materialColor);
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function () {
        if (this.readyState == 4) {
          if (this.status == 200) {
            document.getElementById("message").innerHTML = "<label class=\"msg\">Spool settings saved</label>";
          } else {
            document.getElementById("message").innerHTML = "<label class=\"errormsg\">Error: " + this.responseText + "</label>";
          }
          setTimeout(function () {
            document.getElementById("message").innerHTML = "<label class=\"msg\">&nbsp;</label>";
          }, 5000);
        }
      };
      xhr.open("POST", "/spooldata", true);
      xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
      xhr.send(postdata);
    }
    function loadCache() {
      document.getElementById("materialType").value = localStorage.getItem("materialType");
      document.getElementById("materialWeight").value = localStorage.getItem("materialWeight");
      document.getElementById("materialColor").value = localStorage.getItem("materialColor");
    }
    function clearCache() {
      localStorage.clear();
      document.getElementById("materialType").value = "PLA";
      document.getElementById("materialWeight").value = "1 KG";
      document.getElementById("materialColor").value = "#0000FF";
      sendData();
    }
    function openConf() {
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function () {
        if (this.readyState == 4) {
          if (this.status == 200) {
            if (this.responseText.includes("|-|")) {
              var configData = this.responseText.split("|-|");
              document.getElementById("ap_ssid").value = configData[0];
              document.getElementById("wifi_ssid").value = configData[1];
              document.getElementById("wifi_host").value = configData[2];
            } else {
              document.getElementById("ap_ssid").value = "ACE_RFID";
              document.getElementById("wifi_ssid").value = "";
              document.getElementById("wifi_host").value = "ace.local";
            }
            document.getElementById("cDialog").showModal();
            document.activeElement.blur();
          }
        }
      };
      xhr.open("GET", "/config", true);
      xhr.send();
    }
    function saveConf() {
      var postdata = "ap_ssid=" + document.getElementById("ap_ssid").value + "&ap_pass=" + document.getElementById("ap_pass").value + "&wifi_ssid=" + document.getElementById("wifi_ssid").value + "&wifi_pass=" + document.getElementById("wifi_pass").value + "&wifi_host=" + document.getElementById("wifi_host").value + "&submit";
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function () {
        if (this.readyState == 4) {
          if (this.status == 200) {
            dialog.close();
            document.getElementById("message").innerHTML = "<label class=\"msg\">Config settings saved</label>";
            setTimeout(function () {
              document.getElementById("message").innerHTML = "<label class=\"msg\">&nbsp;</label>";
            }, 5000);
          }
        }
      };
      xhr.open("POST", "/config", true);
      xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
      xhr.send(postdata);
    }
    function loadUpdate() {
      document.getElementById("uDialog").showModal();
      document.activeElement.blur();
    }
    function formatBytes(bytes) {
      if (bytes == 0) return '0 Bytes'; var k = 1024, dm = 2, sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'], i = Math.floor(Math.log(bytes) / Math.log(k)); return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
    }
    function FileSelected(e) {
      file = document.getElementById('fwfile').files[document.getElementById('fwfile').files.length - 1];
      if (file.name.toLowerCase().endsWith(".bin")) {
        var b = file.slice(0, 1);
        var r = new FileReader(); r.onloadend = function (e) {
          if (e.target.readyState === FileReader.DONE) {
            var mb = new Uint8Array(e.target.result);
            if (parseInt(mb[0]) == 233) {
              document.getElementById("selfile").innerHTML = "File: " + file.name + "<br>Size: " + formatBytes(file.size) + "<br>Magic byte: 0x" + parseInt(mb[0]).toString(16).toUpperCase();
              document.getElementById("upload").style.display = "block";
            } else {
              document.getElementById("selfile").innerHTML = "<font color='#df3840'>Invalid firmware file</font><br><br>Magic byte is wrong<br>Expected: 0xE9<br>Found: 0x" + parseInt(mb[0]).toString(16).toUpperCase();
              document.getElementById("upload").style.display = "none";
            }
          }
        }; r.readAsArrayBuffer(b);
      } else {
        document.getElementById("selfile").innerHTML = "<font color='#df3840'>Invalid firmware file</font><br><br>File should be a .bin file";
        document.getElementById("upload").style.display = "none";
      }
    }
    function uploadFile() {
      document.getElementById("upload").style.display = "none";
      document.getElementById("btnsel").style.display = "none";
      document.getElementById("status").innerHTML = "<br><progress max='100' value='0'>0%</progress><br><br>Uploading firmware file";
      var xhr = new XMLHttpRequest();
      xhr.onreadystatechange = function () {
        if (this.readyState === XMLHttpRequest.DONE) {
          if (this.status === 200) {
            document.getElementById("status").innerHTML = this.responseText;
          } else {
            document.getElementById("status").innerHTML = "<font color='#df3840'>HTTP Error</font>";
          }
          setTimeout(redirect, 8000);
        }
      };
      xhr.upload.onprogress = function (e) {
        var percentComplete = Math.ceil((e.loaded / e.total) * 100);
        document.getElementById("status").innerHTML = "<br><progress max='100' value='" + percentComplete + "'>" + percentComplete + "%</progress><br><br>Uploading firmware file";
      };
      xhr.upload.onloadend = function (e) {
        document.getElementById("status").innerHTML = "<div id='loader'></div><br>Updating firmware, Please wait.";
      };
      xhr.open("POST", "/update.html", true);
      var formData = new FormData();
      formData.append("fwfile", file);
      xhr.send(formData);
    }
    function redirect() {
      window.location.href = "/";
    }
    window.addEventListener("DOMContentLoaded", function () {
      loadCache();
    });
  </script>
</head>
<body>
  <center>
    <div class="main">
      <table>
        <tr>
          <td>
          </td>
          <td>
            <br>
            <label style="display: block;text-align: right;">
              <svg xmlns="http://www.w3.org/2000/svg" height="32px" viewBox="0 0 24 24" width="32px"
                onClick="openConf();">
                <path d="M0,0h24v24H0V0z" fill="none" />
                <path
                  d="M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58c0.18-0.14,0.23-0.41,0.12-0.61 l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41 h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87 C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58 c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54 c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94l2.39,0.96 c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6 s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z" />
              </svg>
            </label>
          </td>
        </tr>
        <tr>
          <th colspan="2" style="background-color:#E0E0E0">
            <center>&nbsp;</center>
          </th>
        </tr>
        <tr>
          <td>
            <b>Filament Type:</b>
          </td>
          <td>
            <select name="materialType" id="materialType">
              <option value="ABS">ABS</option>
              <option value="ASA">ASA</option>
              <option value="PETG">PETG</option>
			  <option value="PLA">PLA</option>
			  <option value="PLA Glow">PLA Glow</option>
			  <option value="PLA High Speed">PLA High Speed</option>
			  <option value="PLA Marble">PLA Marble</option>
			  <option value="PLA Matte">PLA Matte</option>
			  <option value="PLA SE">PLA SE</option>
			  <option value="PLA Silk">PLA Silk</option>
			  <option value="TPU">TPU</option>
			</select>
          </td>
        </tr>
        <tr>
          <td>
            <b>Spool Size:</b>
          </td>
          <td>
            <select name="materialWeight" id="materialWeight">
              <option value="1 KG">1 KG</option>
              <option value="750 G">750 G</option>
              <option value="600 G">600 G</option>
              <option value="500 G">500 G</option>
              <option value="250 G">250 G</option>
            </select>
          </td>
        </tr>
        <tr>
          <td>
            <b>Filament Color:</b>
          </td>
          <td>
            <input name="materialColor" id="materialColor" type="color" value="#0000FF" list="commonColors" />
            <datalist id="commonColors">
              <option value="#1200F6" />
              <option value="#3894E1" />
              <option value="#FEFF01" />
              <option value="#F8D531" />
              <option value="#F38E24" />
              <option value="#52D048" />
              <option value="#00FEBE" />
              <option value="#B700F3" />
              <option value="#EE301A" />
              <option value="#FA5959" />
              <option value="#FFFFFF" />
              <option value="#D8D8D8" />
              <option value="#4C4C4C" />
              <option value="#782543" />
              <option value="#000000" />
            </datalist>
          </td>
        </tr>
      </table>
      <br>
      <div id="message"><label class="msg">&nbsp;</label></div>
      <br>
      <input type="submit" class="btn" value="Reset"
        onClick="clearCache()">&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<input type="submit" class="btn" value="Apply"
        onClick="sendData()">
    </div>
  </center>
  <dialog id="cDialog">
    <center>
      <div class="main">
        <table>
          <tr>
            <td></td>
            <td>
              <br>
              <label style="display: block;text-align: right;">
                <svg xmlns="http://www.w3.org/2000/svg" height="32px" viewBox="0 0 24 24" width="32px"
                  onClick="loadUpdate();">
                  <path
                    d="M14,2H6C4.9,2,4.01,2.9,4.01,4L4,20c0,1.1,0.89,2,1.99,2H18c1.1,0,2-0.9,2-2V8L14,2z M18,20H6V4h7v5h5V20z M8,15.01 l1.41,1.41L11,14.84V19h2v-4.16l1.59,1.59L16,15.01L12.01,11L8,15.01z" />
                </svg>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
                <svg xmlns="http://www.w3.org/2000/svg" height="32px" viewBox="0 0 24 24" width="32px"
                  onClick="dialog.close();">
                  <path d="M0 0h24v24H0z" fill="none" />
                  <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
                </svg>
              </label>
            </td>
          </tr>
          <tr>
            <th colspan="2">
              <center>Access Point</center>
            </th>
          </tr>
          <tr>
            <td>AP SSID:</td>
            <td>
              <input size="10" maxlength="32" type="text" id="ap_ssid" value="" autocomplete="off">
            </td>
          </tr>
          <tr>
            <td>AP PASSWORD:</td>
            <td>
              <input size="10" maxlength="63" minlength="8" type="text" id="ap_pass" value="********"
                autocomplete="off">
            </td>
          </tr>
          <tr>
            <th colspan="2">
              <center>Wifi Connection</center>
            </th>
          </tr>
          <tr>
            <td>WIFI SSID:</td>
            <td>
              <input size="10" maxlength="32" type="text" id="wifi_ssid" value="" autocomplete="off">
            </td>
          </tr>
          <tr>
            <td>WIFI PASSWORD:</td>
            <td>
              <input size="10" maxlength="63" minlength="8" type="text" id="wifi_pass" value="********"
                autocomplete="off">
            </td>
          </tr>
          <tr>
            <td>WIFI HOSTNAME:</td>
            <td>
              <input size="10" maxlength="16" type="text" id="wifi_host" value="" autocomplete="off">
            </td>
          </tr>
        </table>
        <br>
        <input id="savecfg" type="submit" class="btn" onClick="saveConf();">
      </div>
    </center>
  </dialog>
  <dialog id="uDialog">
    <center>
      <div class="main">
        <table>
          <tr>
            <td>
              <br>
              <label style="display: block;text-align: right;">
                <svg xmlns="http://www.w3.org/2000/svg" height="32px" viewBox="0 0 24 24" width="32px"
                  onClick="dialogu.close();">
                  <path d="M0 0h24v24H0z" fill="none" />
                  <path d="M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z" />
                </svg>
              </label>
            </td>
          </tr>
          <tr>
            <th colspan="1" width="300">
              <center>Firmware Updater</center>
            </th>
          </tr>
          <tr>
            <td>
              <form>
                <center><input id="btnsel" type="button" class="btn" onclick="document.getElementById('fwfile').click()"
                    value="Select file" style="display: block;"></center>
                <p id="selfile"></p>
                <input id="fwfile" type="file" name="fwupdate" size="0" accept=".bin" onChange="FileSelected();"
                  style="width:0; height:0;"></p>
                <div>
                  <center>
                    <p id="status"></p>
                  </center>
                  <center><input id="upload" type="button" class="btn" value="Update Firmware" onClick="uploadFile();"
                      style="display: none;"></center>
                </div>
              </form>
            </td>
          </tr>
        </table>
      </div>
    </center>
	</dialog>
  <script>
    const dialog = document.getElementById("cDialog");
    const dialogu = document.getElementById("uDialog");
  </script>
</body>
</html>)==";