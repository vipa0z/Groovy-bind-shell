#!/usr/bin/env python3
import os
import base64
import argparse
from pathlib import Path

def chunk_base64_file(input_file, output_dir, chunk_size):
    """Convert a binary file to base64 and split it into chunks."""
    with open(input_file, "rb") as f:
        b64_data = base64.b64encode(f.read()).decode()

    os.makedirs(output_dir, exist_ok=True)

    parts = []
    for i in range(0, len(b64_data), chunk_size):
        chunk = b64_data[i:i + chunk_size]
        part_name = f"part{i // chunk_size + 1}.txt"
        part_path = Path(output_dir) / part_name
        with open(part_path, "w") as out:
            out.write(chunk)
        parts.append(part_name)

    return parts, len(b64_data)


def main():
    parser = argparse.ArgumentParser(
        description="Convert a binary file to base64 and split it into chunk files."
    )
    parser.add_argument("input_file", help="Path to the input binary file (e.g., tool.exe)")
    parser.add_argument("-o", "--output-dir", default="output_chunks", help="Output directory for chunks")
    parser.add_argument(
        "-s", "--chunk-size",
        type=int,
        default=50000,
        help="Length of each chunk string (default: 50000)"
    )
    args = parser.parse_args()

    parts, total_length = chunk_base64_file(args.input_file, args.output_dir, args.chunk_size)

    abs_dir = os.path.abspath(args.output_dir)
    print(f"[+] chunks generated, saved in: {abs_dir}\n")
    print("[+] use the following command to copy all chunks to clipboard:")
    print(f"cat {args.output_dir}/* | xclip -selection clipboard -i\n")
    print("[+] paste the contents of your clipboard into the script console, then add this and edit the path:\n")

    joined_parts = ", ".join(parts)
    java_snippet = f"""import java.util.Base64;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

def allParts = [
    {joined_parts}
].join('');

println "Total base64 length: ${{allParts.length()}}";

def dest = Paths.get("C:/Users/<username>/Desktop/file.exe");
byte[] bytes = Base64.getDecoder().decode(allParts);
Files.write(dest, bytes);
println "Wrote ${{bytes.length}} bytes to ${{dest}}";
"""

    print(java_snippet)


if __name__ == "__main__":
    main()
