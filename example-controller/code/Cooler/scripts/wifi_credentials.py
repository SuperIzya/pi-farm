Import("env")
import os
import sys

sys.path.insert(0, env.subst("$PROJECT_DIR/scripts"))
from default_env import DEFAULTS

wifi_ssid     = os.environ.get("WIFI_SSID",     DEFAULTS.get("WIFI_SSID",     ""))
wifi_password = os.environ.get("WIFI_PASSWORD",  DEFAULTS.get("WIFI_PASSWORD", ""))

env.Append(CPPDEFINES=[
    ("WIFI_SSID",     '\\"' + wifi_ssid     + '\\"'),
    ("WIFI_PASSWORD", '\\"' + wifi_password + '\\"'),
])
