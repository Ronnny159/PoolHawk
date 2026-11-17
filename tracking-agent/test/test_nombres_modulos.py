"""Guardas contra el problema de mayúsculas en nombres de módulos (H-01): Linux las distingue."""
import os

RAIZ = os.path.join(os.path.dirname(__file__), "..")


def test_ningun_modulo_python_tiene_mayusculas_en_su_nombre():
    con_mayusculas = [f for f in os.listdir(RAIZ) if f.endswith(".py") and f != f.lower()]
    assert con_mayusculas == [], f"Renombra a minúsculas (git mv en dos pasos): {con_mayusculas}"


def test_no_hay_modulos_duplicados_dentro_de_test():
    modulos = {f for f in os.listdir(RAIZ) if f.endswith(".py")}
    copias = [f for f in os.listdir(os.path.dirname(__file__))
              if f.endswith(".py") and not f.startswith("test_") and f != "conftest.py"
              and f.lower() in {m.lower() for m in modulos}]
    assert copias == [], f"Copias de módulos dentro de test/ (se importarían en lugar de los reales): {copias}"


def test_todos_los_modulos_se_importan():
    import importlib
    for nombre in ("api_client", "cola_eventos", "ball_classifier", "ball_detector", "ball_tracker",
                   "camera_source", "shot_analyzer", "traza", "calibracion", "config_agente",
                   "vision_agent"):
        importlib.import_module(nombre)
