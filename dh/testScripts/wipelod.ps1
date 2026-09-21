Remove-Item -Recurse -Force "run/client/Distant_Horizons_server_data"

Get-ChildItem -Path "run/server" -Recurse -Filter "Distant_Horizons" | ForEach-Object { Remove-Item -Recurse -Force $_.FullName }
Get-ChildItem -Path "run/server" -Recurse -Filter "DistantHorizons.sqlite" | ForEach-Object { Remove-Item -Force $_.FullName }