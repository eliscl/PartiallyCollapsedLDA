package cc.mallet.types;

import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.math3.exception.NotANumberException;
import static java.lang.Math.abs;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;
import static java.lang.Math.pow;
import static java.lang.Math.exp;

/*
 *  Mathlib : A C Library of Special Functions
 *  Copyright (C) 1998 Ross Ihaka
 *  Copyright (C) 2000-2014 The R Core Team
 *  Copyright (C) 2007 The R Foundation
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, a copy is available at
 *  https://www.R-project.org/Licenses/
 *
 *  SYNOPSIS
 *
 *	#include <Rmath.h>
 *	double rbinom(double nin, double pp)
 *
 *  DESCRIPTION
 *
 *	Random variates from the binomial distribution.
 *
 *  REFERENCE
 *
 *	Kachitvichyanukul, V. and Schmeiser, B. W. (1988).
 *	Binomial random variate generation.
 *	Communications of the ACM 31, 216-222.
 *	(Algorithm BTPEC).
 */

public class BinomialSampler {


	public static int rbinom(double nin, double pp)
	{
		double c=0, fm=0, npq=0, p1=0, p2=0, p3=0, p4=0, qn=0;
		double xl=0, xll=0, xlr=0, xm=0, xr=0;
		
		double psave = -1.0;
		int nsave = -1;
		int m = 0;


		double f, f1, f2, u, v, w, w2, x, x1, x2, z, z2;
		double p, q, np, g, r, al, alv, amaxp, ffm, ynorm;
		int i, ix, k, n;

		if (Double.isNaN(nin)) throw new NotANumberException();
		r = (int) nin;
		if (Double.isInfinite(pp) ||
				/* n=0, p=0, p=1 are not errors <TSL>*/
				r < 0 || pp < 0. || pp > 1.) throw new NotANumberException();

		if (r == 0 || pp == 0.) return 0;
		if (pp == 1.) return (int)r;

		if (r >= Integer.MAX_VALUE)/* evade integer overflow,
				and r == INT_MAX gave only even values */
			throw new IllegalStateException("Num trials is bigger than MAX INT");
		//return qbinom(ThreadLocalRandom.current().nextDouble(), r, pp, /*lower_tail*/ 0, /*log_p*/ 0);
		/* else */
		n = (int) r;

		p = Math.min(pp, 1. - pp);
		q = 1. - p;
		np = n * p;
		r = p / q;
		g = r * (n + 1);

		/* Setup, perform only when parameters change [using static (globals): */

		/* FIXING: Want this thread safe
	       -- use as little (thread globals) as possible
		 */
		if (pp != psave || n != nsave) {
			psave = pp;
			nsave = n;
			if (np < 30.0) {
				/* inverse cdf logic for mean less than 30 */
				qn = pow(q, n);
				return l_np_small(n, g, r, qn, psave);
			} else {
				ffm = np + p;
				m = (int) ffm;
				fm = m;
				npq = np * q;
				p1 = (int)(2.195 * sqrt(npq) - 4.6 * q) + 0.5;
				xm = fm + 0.5;
				xl = xm - p1;
				xr = xm + p1;
				c = 0.134 + 20.5 / (15.3 + fm);
				al = (ffm - xl) / (ffm - xl * p);
				xll = al * (1.0 + 0.5 * al);
				al = (xr - ffm) / (xr * q);
				xlr = al * (1.0 + 0.5 * al);
				p2 = p1 * (1.0 + c + c);
				p3 = p2 + c / xll;
				p4 = p3 + c / xlr;
			}
		} else if (n == nsave) {
			if (np < 30.0)
				return l_np_small(n, g, r, qn, psave);
		}

		/*-------------------------- np = n*p >= 30 : ------------------- */
		for(;;) {
			u = ThreadLocalRandom.current().nextDouble() * p4;
			v = ThreadLocalRandom.current().nextDouble();
			/* triangular region */
			if (u <= p1) {
				ix = (int)(xm - p1 * v + u);
				if (psave > 0.5)
					ix = n - ix;
				return ix;
			}
			/* parallelogram region */
			if (u <= p2) {
				x = xl + (u - p1) / c;
				v = v * c + 1.0 - abs(xm - x) / p1;
				if (v > 1.0 || v <= 0.)
					continue;
				ix = (int) x;
			} else {
				if (u > p3) {	/* right tail */
					ix = (int)(xr - log(v) / xlr);
					if (ix > n)
						continue;
					v = v * (u - p3) * xlr;
				} else {/* left tail */
					ix = (int)(xl + log(v) / xll);
					if (ix < 0)
						continue;
					v = v * (u - p2) * xll;
				}
			}
			/* determine appropriate way to perform accept/reject test */
			k = abs(ix - m);
			if (k <= 20 || k >= npq / 2 - 1) {
				/* explicit evaluation */
				f = 1.0;
				if (m < ix) {
					for (i = m + 1; i <= ix; i++)
						f *= (g / i - r);
				} else if (m != ix) {
					for (i = ix + 1; i <= m; i++)
						f /= (g / i - r);
				}
				if (v <= f) {
					if (psave > 0.5)
						ix = n - ix;
					return ix;
				}
			} else {
				/* squeezing using upper and lower bounds on log(f(x)) */
				amaxp = (k / npq) * ((k * (k / 3. + 0.625) + 0.1666666666666) / npq + 0.5);
				ynorm = -k * k / (2.0 * npq);
				alv = log(v);
				if (alv < ynorm - amaxp) {
					if (psave > 0.5)
						ix = n - ix;
					return ix;
				}
				if (alv <= ynorm + amaxp) {
					/* stirling's formula to machine accuracy */
					/* for the final acceptance/rejection test */
					x1 = ix + 1;
					f1 = fm + 1.0;
					z = n + 1 - fm;
					w = n - ix + 1.0;
					z2 = z * z;
					x2 = x1 * x1;
					f2 = f1 * f1;
					w2 = w * w;
					if (alv <= xm * log(f1 / x1) + (n - m + 0.5) * log(z / w) + (ix - m) * log(w * p / (x1 * q)) + (13860.0 - (462.0 - (132.0 - (99.0 - 140.0 / f2) / f2) / f2) / f2) / f1 / 166320.0 + (13860.0 - (462.0 - (132.0 - (99.0 - 140.0 / z2) / z2) / z2) / z2) / z / 166320.0 + (13860.0 - (462.0 - (132.0 - (99.0 - 140.0 / x2) / x2) / x2) / x2) / x1 / 166320.0 + (13860.0 - (462.0 - (132.0 - (99.0 - 140.0 / w2) / w2) / w2) / w2) / w / 166320.)
					{
						if (psave > 0.5)
							ix = n - ix;
						return ix;
					}
				}
			}
		}

		//		if (psave > 0.5)
		//			ix = n - ix;
		//		return (double)ix;
	}

	static int l_np_small(int n, double g, double r, double qn, double psave) {
		int ix;
		for(;;) {
			ix = 0;
			double f = qn;
			double u = ThreadLocalRandom.current().nextDouble();
			for(;;) {
				if (u < f) {
					if (psave > 0.5)
						ix = n - ix;
					return ix;
				}
				if (ix > 110)
					break;
				u -= f;
				ix++;
				f *= (g / ix - r);
			}
		}

		//		if (psave > 0.5)
		//			ix = n - ix;
		//		return (double)ix;
	}
}