// Roda sozinho depois do "npx cap sync": copia o widget para o projeto Android
const fs = require('fs'), path = require('path');
const root = path.join(__dirname, '..');
const android = path.join(root, 'android', 'app');
if (!fs.existsSync(android)) { console.log('widget: pasta android ainda não existe'); process.exit(0); }
fs.cpSync(path.join(__dirname, 'app'), android, { recursive: true });
const mf = path.join(android, 'src', 'main', 'AndroidManifest.xml');
let s = fs.readFileSync(mf, 'utf8');
if (!s.includes('GatoWidget')) {
  s = s.replace('</application>', fs.readFileSync(path.join(__dirname, 'receiver.xml'), 'utf8') + '    </application>');
  fs.writeFileSync(mf, s);
}
// Mesma chave de assinatura em todo build: permite instalar atualizações por cima sem desinstalar
const os = require('os');
const ks = path.join(os.homedir(), '.android');
fs.mkdirSync(ks, { recursive: true });
fs.copyFileSync(path.join(__dirname, 'debug.keystore'), path.join(ks, 'debug.keystore'));
console.log('widget: instalado');
