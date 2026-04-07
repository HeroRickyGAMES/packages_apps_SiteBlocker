# SiteBlocker — ROM-level site blocker for AOSP/LineageOS/EvolutionX

Bloqueador de sites integrado ao sistema, funciona para **todos os apps** (não só browser), sem VPN, sem root pós-build. O usuário adiciona e remove domínios pela UI.

---

## Como funciona

```
App (qualquer) → InetAddress.getByName("exemplo.com")
                    ↓
         Inet6AddressImpl.lookupAllHostAddr()  ← patch aplicado aqui
                    ↓
         lê /data/system/site_blocker_domains  ← arquivo escrito pela UI
                    ↓
         domínio bloqueado? → UnknownHostException
         senão             → resolução DNS normal
```

- **Arquivo** em vez de `Settings.Global` — evita problemas de permissão em processos não-app (daemons, early-boot).
- Cache com TTL de 5s baseado em `lastModified()` — sem I/O a cada lookup.
- Bloqueio de subdomínios automático: bloquear `exemplo.com` também bloqueia `www.exemplo.com`, `api.exemplo.com`, etc.
- Funciona para **qualquer app**: browsers, clientes de API, jogos, etc.

---

## Requisitos

| Item | Versão mínima |
|---|---|
| Android | 13+ (testado no 16) |
| ROM base | LineageOS, EvolutionX, AOSP puro |
| Assinatura | `platform` (certificado da ROM) |

---

## Instalação

### 1. Clone na árvore AOSP

```bash
# Dentro do seu AOSP root:
git clone https://github.com/SEU_USER/android_packages_apps_SiteBlocker \
    packages/apps/SiteBlocker
```

Ou via `.repo/local_manifests/siteblocker.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<manifest>
  <remote name="github" fetch="https://github.com/SEU_USER" />
  <project name="android_packages_apps_SiteBlocker"
           path="packages/apps/SiteBlocker"
           remote="github"
           revision="main" />
</manifest>
```

### 2. Aplique o patch no libcore

```bash
cd packages/apps/SiteBlocker
bash setup.sh ../../../
```

O script aplica `patches/0001-libcore-Inet6AddressImpl-site-blocker.patch` ao arquivo:
`libcore/ojluni/src/main/java/java/net/Inet6AddressImpl.java`

Se preferir aplicar manualmente:
```bash
patch -p1 -d . < packages/apps/SiteBlocker/patches/0001-libcore-Inet6AddressImpl-site-blocker.patch
```

### 3. Adicione ao PRODUCT_PACKAGES

Em `device/<vendor>/<device>/device.mk` **ou** em `vendor/<sua_rom>/config/common_packages.mk`:
```makefile
PRODUCT_PACKAGES += SiteBlocker
```

### 4. Build

```bash
# Só o app + libcore (rebuild incremental):
m SiteBlocker

# Build completo:
m
```

---

## Como usar (usuário final)

1. Abrir **Bloqueador de Sites** no app drawer.
2. Toque no botão **+** para adicionar um domínio (ex: `facebook.com`).
3. Subdomínios são bloqueados automaticamente.
4. Para remover, toque no ícone de lixeira ao lado do domínio.
5. Menu ⋮ → **Limpar tudo** para zerar a lista.

O bloqueio tem efeito em **até 5 segundos** após a mudança.

---

## Integração com o menu de Configurações

Para adicionar uma entrada em Configurações → Rede e Internet, inclua em
`packages/apps/Settings/res/xml/network_provider_settings.xml`:

```xml
<Preference
    android:key="site_blocker"
    android:title="Bloqueador de Sites"
    android:summary="Gerenciar domínios bloqueados">
    <intent
        android:action="com.android.siteblocker.ACTION_MANAGE"
        android:targetPackage="com.android.siteblocker"
        android:targetClass="com.android.siteblocker.SiteBlockerActivity" />
</Preference>
```

---

## Estrutura do repositório

```
SiteBlocker/
├── Android.bp                        # build module (privileged system app)
├── AndroidManifest.xml               # sharedUserId=android.uid.system
├── setup.sh                          # aplica patches, imprime próximos passos
├── patches/
│   └── 0001-libcore-Inet6AddressImpl-site-blocker.patch
├── privapp-permissions/
│   └── privapp-permissions-SiteBlocker.xml
├── res/
│   ├── drawable/ic_site_blocker.xml
│   ├── layout/{activity_site_blocker,item_domain,dialog_add_domain}.xml
│   └── values/strings.xml
└── src/com/android/siteblocker/
    ├── SiteBlockerActivity.java      # UI principal
    └── SiteBlockerProvider.java      # ContentProvider stub (expansão futura)
```

---

## Segurança / Notas

- O arquivo `/data/system/site_blocker_domains` é **world-readable** (leitura por qualquer processo) mas só pode ser escrito por UID 1000 (system). Outros apps não conseguem modificar a lista.
- O patch no `libcore` opera **antes** da resolução DNS nativa, então nem proxies nem HTTPS contornam o bloqueio (o socket nunca é aberto).
- **Limitação:** apps que usam DoH (DNS-over-HTTPS) embutido (ex: algumas versões do Chrome com fallback) podem contornar. Para bloquear DoH, adicione também os endpoints DoH à lista (ex: `dns.google`, `cloudflare-dns.com`).

---

## Licença

Apache 2.0 — veja `NOTICE`.
