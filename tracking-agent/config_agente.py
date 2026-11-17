"""
Configuración única y versionada del agente (H-06 de la auditoría de ``2088938``).

Un solo archivo JSON liga, para la MISMA resolución de referencia: el ROI de la mesa, las
coordenadas de las troneras, los parámetros del detector y los umbrales del clasificador. El
agente lo valida contra la resolución real de la fuente antes de usarlo:

* misma proporción de imagen -> ROI y troneras se reescalan automáticamente;
* distinta proporción (otro encuadre) -> se rechaza: hay que recalibrar.

Formato (``version`` = 1)::

    {"version": 1,
     "resolucion_referencia": [1280, 720],
     "roi": [80, 40, 1100, 640],
     "troneras": {"TOP_LEFT": [60, 50], "...": [0, 0]},
     "detector": {"param2": 28, "min_radius": 18, "max_radius": 40},
     "clasificador": {"UMBRAL_RAYADA": 0.12}}

Los radios y distancias de ``detector`` se expresan en píxeles de la resolución de referencia.
"""
from __future__ import annotations

import json
from dataclasses import dataclass, field, fields, replace
from typing import Dict, Optional, Tuple

from ball_classifier import _UMBRALES_CONFIGURABLES
from ball_detector import ConfigDetector

VERSION = 1
TOLERANCIA_PROPORCION = 0.02  # diferencia relativa máxima entre ancho/alto de referencia y reales
Punto = Tuple[float, float]
_CAMPOS_DETECTOR = {f.name for f in fields(ConfigDetector)} - {"roi_mesa"}
_ESCALABLES = ("min_dist", "min_radius", "max_radius")


class ErrorConfig(ValueError):
    """La configuración es inválida o no es compatible con la fuente de vídeo actual."""


@dataclass
class ConfigAgente:
    """Calibración completa del agente para una resolución de referencia."""

    resolucion_referencia: Optional[Tuple[int, int]] = None
    roi: Optional[Tuple[int, int, int, int]] = None
    troneras: Dict[str, Punto] = field(default_factory=dict)
    detector: Dict[str, float] = field(default_factory=dict)
    clasificador: Dict[str, float] = field(default_factory=dict)

    # ------------------------------------------------------------------ E/S
    @classmethod
    def cargar(cls, ruta: str) -> "ConfigAgente":
        """Lee y valida un archivo de configuración.

        Raises:
            ErrorConfig: si el archivo no existe, no es JSON o su contenido no es válido.
        """
        try:
            with open(ruta, encoding="utf-8") as f:
                datos = json.load(f)
        except OSError as e:
            raise ErrorConfig(f"No se pudo leer la configuración {ruta}: {e}") from e
        except ValueError as e:
            raise ErrorConfig(f"La configuración {ruta} no es JSON válido") from e
        if not isinstance(datos, dict) or datos.get("version") != VERSION:
            raise ErrorConfig(f"Versión de configuración no soportada (se esperaba {VERSION})")
        try:
            res = datos.get("resolucion_referencia")
            roi = datos.get("roi")
            cfg = cls(
                resolucion_referencia=(int(res[0]), int(res[1])) if res else None,
                roi=tuple(int(v) for v in roi) if roi else None,  # type: ignore[arg-type]
                troneras={k: (float(v[0]), float(v[1])) for k, v in datos.get("troneras", {}).items()},
                detector=dict(datos.get("detector", {})),
                clasificador=dict(datos.get("clasificador", {})),
            )
        except (TypeError, ValueError, IndexError) as e:
            raise ErrorConfig(f"Estructura de configuración inválida: {e}") from e
        cfg.validar()
        return cfg

    def guardar(self, ruta: str) -> None:
        """Valida y escribe la configuración."""
        self.validar()
        with open(ruta, "w", encoding="utf-8") as f:
            json.dump({
                "version": VERSION,
                "resolucion_referencia": list(self.resolucion_referencia) if self.resolucion_referencia else None,
                "roi": list(self.roi) if self.roi else None,
                "troneras": {k: [v[0], v[1]] for k, v in self.troneras.items()},
                "detector": self.detector,
                "clasificador": self.clasificador,
            }, f, indent=2, ensure_ascii=False)

    def validar(self) -> None:
        """Comprueba coherencia interna (resolución, claves conocidas y rangos)."""
        if self.resolucion_referencia is not None and min(self.resolucion_referencia) <= 0:
            raise ErrorConfig("resolucion_referencia debe ser positiva")
        if (self.roi or self.troneras) and self.resolucion_referencia is None:
            raise ErrorConfig("El ROI y las troneras requieren 'resolucion_referencia'")
        for clave in self.detector:
            if clave not in _CAMPOS_DETECTOR:
                raise ErrorConfig(f"Parámetro de detector desconocido: {clave}")
        for clave in self.clasificador:
            if clave not in _UMBRALES_CONFIGURABLES:
                raise ErrorConfig(f"Umbral de clasificador desconocido: {clave}")

    # ------------------------------------------------------------------ uso
    def para_resolucion(self, ancho: int, alto: int) -> "ConfigAgente":
        """Devuelve la configuración adaptada a la resolución real de la fuente.

        Raises:
            ErrorConfig: si la proporción de imagen difiere de la calibrada (hay que recalibrar).
        """
        if self.resolucion_referencia is None:
            return self
        ref_w, ref_h = self.resolucion_referencia
        kx, ky = ancho / ref_w, alto / ref_h
        if abs(kx - ky) / max(kx, ky) > TOLERANCIA_PROPORCION:
            raise ErrorConfig(
                f"La fuente es {ancho}x{alto} pero la calibración es de {ref_w}x{ref_h}: la proporción "
                "no coincide (otro encuadre o recorte). Recalibra con calibracion.py")
        k = kx
        return replace(
            self,
            resolucion_referencia=(ancho, alto),
            roi=tuple(round(v * k) for v in self.roi) if self.roi else None,  # type: ignore[arg-type]
            troneras={n: (x * k, y * k) for n, (x, y) in self.troneras.items()},
            detector={n: (round(v * k) if n in _ESCALABLES else v) for n, v in self.detector.items()},
        )

    def config_detector(self) -> ConfigDetector:
        """Construye el ``ConfigDetector`` (los parámetros ya están en la resolución de la fuente)."""
        try:
            return replace(ConfigDetector(), roi_mesa=self.roi, **self.detector)
        except TypeError as e:
            raise ErrorConfig(f"Parámetros de detector inválidos: {e}") from e


def guardar_calibracion(ruta: str, resolucion: Tuple[int, int], **campos) -> ConfigAgente:
    """Actualiza (o crea) la configuración con nuevos datos de calibración.

    Si ya había calibración hecha a OTRA resolución se rechaza: mezclar coordenadas de dos
    resoluciones en un mismo archivo las haría inconsistentes.

    Raises:
        ErrorConfig: si la resolución no coincide con la del archivo existente.
    """
    try:
        cfg = ConfigAgente.cargar(ruta)
    except ErrorConfig as e:
        if "No se pudo leer" not in str(e):
            raise
        cfg = ConfigAgente()
    tiene_datos = bool(cfg.roi or cfg.troneras)
    if cfg.resolucion_referencia not in (None, tuple(resolucion)) and tiene_datos:
        raise ErrorConfig(
            f"{ruta} ya tiene una calibración a {cfg.resolucion_referencia[0]}x{cfg.resolucion_referencia[1]}; "
            f"calibrar a {resolucion[0]}x{resolucion[1]} la dejaría inconsistente. Usa otro archivo")
    cfg.resolucion_referencia = tuple(resolucion)
    for nombre, valor in campos.items():
        setattr(cfg, nombre, valor)
    cfg.guardar(ruta)
    return cfg
