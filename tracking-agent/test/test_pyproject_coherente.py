"""El contrato de empaquetado debe coincidir con los archivos reales (H-02 de la auditoría a1440c5)."""
import os

import pytest

tomllib = pytest.importorskip("tomllib")  # Python >= 3.11 (el CI usa 3.12)

RAIZ = os.path.join(os.path.dirname(__file__), "..")
NO_DISTRIBUIDOS = {"simulador"}  # script independiente, no un módulo del agente


def _declarados():
    with open(os.path.join(RAIZ, "pyproject.toml"), "rb") as f:
        return set(tomllib.load(f)["tool"]["setuptools"]["py-modules"])


def test_el_archivo_se_llama_exactamente_pyproject_toml():
    assert "pyproject.toml" in os.listdir(RAIZ)
    assert not [f for f in os.listdir(RAIZ) if f.lower() == "pyproject.toml" and f != "pyproject.toml"]


def test_todo_modulo_declarado_existe_como_archivo():
    for modulo in _declarados():
        assert os.path.isfile(os.path.join(RAIZ, f"{modulo}.py")), f"{modulo}.py no existe"


def test_todo_modulo_del_agente_esta_declarado():
    existentes = {f[:-3] for f in os.listdir(RAIZ) if f.endswith(".py")} - NO_DISTRIBUIDOS
    assert existentes == _declarados()
