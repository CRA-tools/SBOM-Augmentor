import sys
import zipfile

# From: https://stackoverflow.com/a/43808895
if __name__ == "__main__":
    wheel_to_inspect = sys.argv[1]
    zf = zipfile.ZipFile(wheel_to_inspect)
    top_level = set([x.split('/')[0] for x in zf.namelist()])
    top_level = [x for x in top_level if not x.endswith('.dist-info')]
    for dist in top_level:
        print(dist)