Import("env")
import os

def quoted(value):
    return '\\"' + value.replace("\\", "\\\\").replace('"', '\\\\\\"') + '\\"'


def integer(name, minimum, maximum):
    value = int(os.environ[name])
    if not minimum <= value <= maximum:
        raise ValueError(f"{name} must be between {minimum} and {maximum}")
    return value


env.Append(CPPDEFINES=[
    ("WIFI_SSID", quoted(os.environ["WIFI_SSID"])),
    ("WIFI_PASSWORD", quoted(os.environ["WIFI_PASSWORD"])),
    ("CONTROLLER_ID", integer("CONTROLLER_ID", 1, 2147483647)),
    ("UDP_PORT", integer("UDP_PORT", 1, 65535)),
])
