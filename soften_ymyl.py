import json
import re

path = r'c:\Development\Owner\OfferVerdict\src\main\resources\data\city-context.json'
with open(path, 'r', encoding='utf-8') as f:
    data = json.load(f)

for city, info in data.items():
    for key, text in info.items():
        text = re.sub(r'\(January 2026\)', '(recent estimates)', text)
        text = re.sub(r'in 2025', 'recently', text)
        text = re.sub(r'2026', 'recent years', text)
        text = re.sub(r'Zillow\'s Rent Index', 'industry rent indices', text)
        text = re.sub(r'Zillow data', 'Recent housing data', text)
        text = re.sub(r'According to Zillow', 'According to recent housing data', text)
        text = re.sub(r'Forbes.*? ', 'major financial publications ', text)
        text = re.sub(r'Numbeo( data)?', 'Cost of living aggregators', text)
        text = re.sub(r'Redfin data shows', 'Market trends indicate', text)
        text = re.sub(r'Redfin,', 'real estate trends,', text)
        info[key] = text

with open(path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4)
print('YMYL text softened.')
