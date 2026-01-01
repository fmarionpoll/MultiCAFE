# Increasing Java Heap Size for MultiCAFE

If you encounter `OutOfMemoryError` when building kymographs, you need to increase the Java heap size for ICY.

## How to Increase Heap Size

### Method 1: ICY Configuration File (Recommended)

1. Find ICY's installation directory
2. Look for a file named `icy.vmoptions` or `icy64.vmoptions` (or similar)
3. If the file doesn't exist, create it in the ICY installation directory
4. Add or modify the following line:
   ```
   -Xmx4g
   ```
   (Use `-Xmx8g` for 8GB, `-Xmx16g` for 16GB, etc. Adjust based on your system's RAM)

5. Restart ICY

### Method 2: ICY Launcher Script

If ICY is launched via a script (e.g., `icy.sh` on Linux/Mac or `icy.bat` on Windows):

1. Edit the launcher script
2. Find the line that starts Java (usually contains `java` or `javaw`)
3. Add `-Xmx4g` (or your desired size) to the Java command
4. Example:
   ```bash
   java -Xmx4g -jar icy.jar
   ```

### Method 3: Windows Shortcut

If you launch ICY from a Windows shortcut:

1. Right-click the ICY shortcut
2. Select "Properties"
3. In the "Target" field, add `-Xmx4g` before the program path
4. Example:
   ```
   "C:\Program Files\Java\bin\javaw.exe" -Xmx4g -jar "C:\Program Files\ICY\icy.jar"
   ```

## Recommended Heap Sizes

- **Small datasets** (< 10 experiments, < 50 capillaries each): 2GB (`-Xmx2g`)
- **Medium datasets** (10-50 experiments): 4GB (`-Xmx4g`)
- **Large datasets** (50+ experiments): 8GB (`-Xmx8g`) or more

## Notes

- Ensure your system has enough RAM. If you set `-Xmx4g`, your system should have at least 8GB total RAM
- The heap size cannot exceed your system's available RAM
- After changing heap size, you must restart ICY for changes to take effect

## If Problems Persist

If you still encounter memory errors after increasing heap size:
- Check the error message for the specific memory requirement
- Consider processing fewer experiments at a time
- Check for other applications using significant memory
- Verify your system has adequate RAM available

