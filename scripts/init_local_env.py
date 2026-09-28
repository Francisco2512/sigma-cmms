"""Genera backend/.env y database/setup-local.sql con secretos aleatorios.

No imprime ningun secreto. Si backend/.env ya existe, lo reutiliza.
Uso:  python scripts/init_local_env.py
"""
import secrets
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ENV = ROOT / "backend" / ".env"
TEMPLATE = ROOT / "database" / "setup.template.sql"
SETUP = ROOT / "database" / "setup-local.sql"


def read_env(path):
    values = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.lstrip().startswith("#"):
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip()
    return values


def main():
    if ENV.exists():
        values = read_env(ENV)
        print("backend/.env ya existe: se reutiliza")
    else:
        values = {
            "SIGMA_DB_URL": "jdbc:mysql://localhost:3306/sigma_cmms",
            "SIGMA_DB_USER": "sigma",
            "SIGMA_DB_PASSWORD": secrets.token_urlsafe(18),
            "SIGMA_JWT_SECRET": secrets.token_urlsafe(48),
            "SIGMA_DEMO_PASSWORD": "Sigma-" + secrets.token_hex(3),
        }
        ENV.write_text("".join(f"{k}={v}\n" for k, v in values.items()), encoding="utf-8")
        print("backend/.env creado")
    sql = TEMPLATE.read_text(encoding="utf-8").replace("{{DB_PASSWORD}}", values["SIGMA_DB_PASSWORD"])
    SETUP.write_text(sql, encoding="utf-8")
    print("database/setup-local.sql creado")
    print("Siguiente paso:  mysql -u root -p < database/setup-local.sql")
    print("La contrasena de los usuarios de demostracion esta en backend/.env (SIGMA_DEMO_PASSWORD)")


if __name__ == "__main__":
    main()
