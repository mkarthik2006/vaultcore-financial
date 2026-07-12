#!/usr/bin/env bash
# One-shot OS bootstrap for an Oracle Cloud Ubuntu 24.04 (Ampere A1 / ARM64) VM.
# Installs Docker, nginx, certbot, firewall, fail2ban, SSH hardening, auto-updates, swap.
# Run as a sudo-capable user:  bash deploy/scripts/provision.sh
set -euo pipefail

echo "==> Updating OS"
sudo apt-get update -y && sudo apt-get upgrade -y

echo "==> Base packages"
sudo apt-get install -y ca-certificates curl git ufw fail2ban unattended-upgrades nginx \
    certbot python3-certbot-nginx openjdk-21-jdk-headless

echo "==> Docker Engine + Compose plugin (official repo, arm64)"
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
    | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update -y
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker "$USER"

echo "==> Swap (4G) — safety margin for Keycloak's build/augmentation"
if ! sudo swapon --show | grep -q swapfile; then
  sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile
  sudo mkswap /swapfile && sudo swapon /swapfile
  echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
fi

echo "==> Firewall (UFW): allow 22/80/443 only; block everything else"
sudo ufw --force reset
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw --force enable

echo "==> SSH hardening (key-only, no root login)"
sudo sed -i 's/^#\?PermitRootLogin.*/PermitRootLogin no/' /etc/ssh/sshd_config
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo systemctl restart ssh || sudo systemctl restart sshd

echo "==> fail2ban (sshd jail)"
sudo systemctl enable --now fail2ban

echo "==> Unattended security upgrades"
echo 'Unattended-Upgrade::Automatic-Reboot "false";' | sudo tee /etc/apt/apt.conf.d/51unattended-reboot >/dev/null
sudo systemctl enable --now unattended-upgrades

echo "==> ACME webroot"
sudo mkdir -p /var/www/certbot

echo "==> Enable Docker on boot"
sudo systemctl enable --now docker

cat <<'DONE'

Provisioning complete.
NEXT:
  1) Log out / back in (so your user picks up the 'docker' group).
  2) Clone the repo, create .env (see deploy/env/.env.prod.example).
  3) Install the host nginx config + obtain certs (see docs/deployment/oracle-cloud.md).
  4) Run deploy/scripts/deploy.sh
DONE
