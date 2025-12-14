# MikroTik API All-In-One Extension

Extensión **All-in-One** para **MIT App Inventor / Kodular / Niotron** que permite
controlar **routers MikroTik (RouterOS 6 y 7+)** mediante la **API oficial**,
incluyendo **SSL**, **Hotspot**, **perfiles (fichas)** y **control de sesiones activas**.

Pensada para:
- WISP rurales
- venta de fichas hotspot
- apps de vendedores
- automatización sin WinBox

---

## 🚀 Características

- ✅ Conexión API MikroTik (8728 / 8729 SSL)
- 🔐 Soporte **SSL** (RouterOS 7+)
- 👥 Gestión de usuarios Hotspot
- 🎟️ Perfiles de fichas (tiempo / velocidad)
- 🥾 Control de sesiones activas (kick)
- 🧠 Información del sistema (CPU, RAM, identidad)
- 📤 Respuestas en **JSON limpio**
- 🧩 Arquitectura modular y extensible

---

## 🧱 Arquitectura del proyecto

```
extension/
├─ com/sub7corp/mikrotikapi/
│  ├─ MikrotikApiExtension.java
│  ├─ core/
│  │  ├─ MkConnection.java
│  │  ├─ MkClient.java
│  │  └─ MkResponse.java
│  └─ api/
│     ├─ HotspotApi.java
│     ├─ ProfileApi.java
│     ├─ ActiveApi.java
│     └─ SystemApi.java
```

---

## 🔌 Conexión básica

1. Configura:
   - Host
   - Port (8728 / 8729)
   - UseSSL
   - Username / Password
2. Ejecuta `Connect`
3. Escucha el evento `OnConnected`

---

## 📦 Bloques principales

### Hotspot
- HotspotListUsers
- HotspotListActive
- HotspotAddUser
- HotspotRemoveUser

### Perfiles
- ProfileList
- ProfileAdd
- ProfileSet
- ProfileRemove

### Activos
- ActiveList
- ActiveKickUser

### Sistema
- SystemGetIdentity
- SystemGetResources
- SystemGetClock
- SystemGetRouterBoard

---

## 📤 Respuesta JSON

```json
{
  "success": true,
  "error": false,
  "message": "",
  "records": []
}
```

---

## 🧑‍💻 Autor

sub7corp / Yorllhy Yankovich Estrada  
México 🇲🇽

---

## 📜 Licencia

MIT License
