import os
from PIL import Image, ImageDraw, ImageFont

def create_image(filename, text):
    img = Image.new('RGB', (800, 600), color = (30, 30, 30))
    d = ImageDraw.Draw(img)
    # Just draw text in the middle
    d.text((50, 280), text, fill=(200, 200, 200))
    img.save(f"docs/assets/{filename}")

os.makedirs("docs/assets", exist_ok=True)
create_image("simulator.png", "ISO8583 Simulator (Placeholder Screenshot)")
create_image("inspector.png", "State Inspector (Placeholder Screenshot)")
create_image("sandbox-flow.png", "Banking Sandbox Flow (Placeholder Screenshot)")
