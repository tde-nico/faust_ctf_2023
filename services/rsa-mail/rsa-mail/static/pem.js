function parse_int(bytes, start, length) {
	if (length < 4) {
		let num = 0;
		for (let i = 0; i < length; i++) {
			num <<= 8;
			num += bytes[start + i];
		}
		return num;
	} else {
		return new BigInteger(hex(bytes.slice(start, start + length)), 16);
	}
}

function parse_asn1(bytes, start, end, sequence=false) {
	let data = [];

	while (start < end) {
		let tag = bytes[start];
		let type = tag & 0x1F;

		let length = bytes[start + 1];
		start += 2;
		if ((length & 0x80) != 0) { // long form
			let lenbytes = length - 0x80;
			length = parse_int(bytes, start, lenbytes);
			start += lenbytes;
		}

		if ((tag & 0x20) == 0) { // Primitive element
			switch (type) {
			case 0x2:
				data.push(parse_int(bytes, start, length));
				break;
			case 0x5:
				data.push([]);
				break;
			default:
				data.push(bytes.slice(start, start + length))
			}
		} else { // Constructed element
			switch (type) {
			case 0x10:
				data.push(parse_asn1(bytes, start, start + length, true));
				break;
			}
		}

		start += length;
		if (!sequence) {
			break;
		}
	}

	if (sequence) {
		return data;
	} else {
		return data[0] || null;
	}
}

function cmp_bytes(a, b) {
	if (a.length != b.length) {
		return false;
	}

	for (let i = 0; i < a.length; i++) {
		if (a[i] != b[i]) {
			return false;
		}
	}

	return true;
}

function parse_private_key(data) {
	if (!data.startsWith('-----BEGIN PRIVATE KEY-----\n') || !data.endsWith('\n-----END PRIVATE KEY-----\n')) {
		return [null, "Invalid Key"];
	}
	let lines = data.trim().split('\n');
	let b64data = lines.slice(1, -1).join('');
	let encoded = base64DecToArr(b64data);

	let parsed = parse_asn1(encoded, 0, encoded.length);
	if (parsed[0] != 0) {
		return [null, "Invalid Key"];
	}

	if (!cmp_bytes(parsed[1][0], [42, 134, 72, 134, 247, 13, 1, 1, 1])) {
		return [null, "Not an RSA Key"];
	}

	private_key = parse_asn1(parsed[2], 0, parsed[2].length);

	let n = private_key[1];
	let e = private_key[2];
	let d = private_key[3];
	
	return [[n.toString(16), e.toString(16), d.toString(16)], "Success"];
}



// Taken from https://developer.mozilla.org/en-US/docs/Glossary/Base64
// Array of bytes to Base64 string decoding
function b64ToUint6(nChr) {
	return nChr > 64 && nChr < 91 ?
		nChr - 65
		: nChr > 96 && nChr < 123 ?
		nChr - 71
		: nChr > 47 && nChr < 58 ?
		nChr + 4
		: nChr === 43 ?
		62
		: nChr === 47 ?
		63
		:
		0;
}

function base64DecToArr(sBase64, nBlocksSize){ 
	const sB64Enc = sBase64.replace(/[^A-Za-z0-9+/]/g, "");
	const nInLen = sB64Enc.length;
	const nOutLen = nBlocksSize ? Math.ceil((nInLen * 3 + 1 >> 2) / nBlocksSize) * nBlocksSize : nInLen * 3 + 1 >> 2;
	const taBytes = new Uint8Array(nOutLen);

	let nMod3;
	let nMod4;
	let nUint24 = 0;
	let nOutIdx = 0;
	for (let nInIdx = 0; nInIdx < nInLen; nInIdx++) {
		nMod4 = nInIdx & 3;
		nUint24 |= b64ToUint6(sB64Enc.charCodeAt(nInIdx)) << 6 * (3 - nMod4);
		if (nMod4 === 3 || nInLen - nInIdx === 1) {
			nMod3= 0;
			while (nMod3 < 3 && nOutIdx < nOutLen) {
				taBytes[nOutIdx] = nUint24 >>> (16 >>> nMod3 & 24) & 255;
				nMod3++;
				nOutIdx++;
			}
			nUint24 = 0;

		}
	}

	return taBytes;
}
